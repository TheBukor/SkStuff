package me.TheBukor.SkStuff.expressions;

import java.util.Arrays;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.util.Slot;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import me.TheBukor.SkStuff.SkStuff;
import me.TheBukor.SkStuff.util.ReflectionUtils;

public class ExprNBTOf extends SimpleExpression<Object> {
	private Expression<Object> target;

	private Class<?> nbtClass = ReflectionUtils.getNMSClass("NBTTagCompound", false);

	@Override
	public Class<? extends Object> getReturnType() {
		return nbtClass;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expr, int matchedPattern, Kleenean arg2, ParseResult result) {
		target = (Expression<Object>) expr[0];
		Class<?> type = target.getReturnType();
		Class<? extends Event>[] evts = ScriptLoader.getCurrentEvents();
		for (int i = 0; i < evts.length; i++) {
			Bukkit.broadcastMessage("Event #" + i + ": \u00A7b" + evts[i].getSimpleName());
		}
		Bukkit.broadcastMessage("Object type: \u00A79" + type.getSimpleName());
		if (type != Entity.class || type != Block.class || type != ItemStack.class || type != Slot.class) {
			Skript.error(target.toString() + " is neither an entity, a block nor an itemstack.", ErrorQuality.SEMANTIC_ERROR);
		}
		return true;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "the NBT of " + target.toString(e, debug);
	}

	@Override
	@Nullable
	public Object[] get(Event e) {
		Object tar = target.getSingle(e);
		if (tar instanceof Entity) {
			return new Object[] { SkStuff.getNMSMethods().getEntityNBT((Entity) tar) };
		} else if (tar instanceof Block) {
			return new Object[] { SkStuff.getNMSMethods().getTileNBT((Block) tar) };
		} else if (tar instanceof ItemStack) {
			return new Object[] { SkStuff.getNMSMethods().getItemNBT((ItemStack) tar) };
		}
		return null;
	}

	@Override
	public void change(Event e, Object[] delta, ChangeMode mode) {
		Object tar = target.getSingle(e);
		if (tar instanceof Entity) {
			Object entNBT = SkStuff.getNMSMethods().getEntityNBT((Entity) tar);
			if (mode == ChangeMode.ADD) {
				Object parsedNBT = SkStuff.getNMSMethods().parseRawNBT((String) delta[0]);
				SkStuff.getNMSMethods().removeFromCompound(parsedNBT, "UUIDMost", "UUIDLeast", "WorldUUDMost", "WorldUUIDLeast", "Bukkit.updateLevel");
				SkStuff.getNMSMethods().addToCompound(entNBT, parsedNBT);
				SkStuff.getNMSMethods().setEntityNBT((Entity) tar, entNBT);
			} else if (mode == ChangeMode.REMOVE) {
				for (Object s : delta) {
					if (s != "UUIDMost" || s != "UUIDLeast" || s != "WorldUUIDMost" || s != "WorldUUIDLeast" || s != "Bukkit.updateLevel") { // Prevent crucial data from being modified
						SkStuff.getNMSMethods().removeFromCompound(entNBT, (String) s);
					}
				}
				SkStuff.getNMSMethods().setEntityNBT((Entity) tar, entNBT);
			}
		} else if (tar instanceof Block) {
			Object blockNBT = SkStuff.getNMSMethods().getTileNBT((Block) tar);
			if (mode == ChangeMode.ADD) {
				Object parsedNBT = SkStuff.getNMSMethods().parseRawNBT((String) delta[0]);
				SkStuff.getNMSMethods().removeFromCompound(parsedNBT, "x", "y", "z", "id");
				SkStuff.getNMSMethods().addToCompound(blockNBT, parsedNBT);
				SkStuff.getNMSMethods().setTileNBT((Block) tar, blockNBT);
			} else if (mode == ChangeMode.REMOVE) {
				for (Object s : delta) {
					if (s != "x" || s != "y" || s != "z" || s != "id") {
						SkStuff.getNMSMethods().removeFromCompound(blockNBT, (String) s);
					}
				}
				SkStuff.getNMSMethods().setTileNBT((Block) tar, blockNBT);
			}
		} else if (tar instanceof ItemStack) {
			Object itemNBT = SkStuff.getNMSMethods().getItemNBT((ItemStack) tar);
			if (mode == ChangeMode.ADD) {
				Object parsedNBT = SkStuff.getNMSMethods().parseRawNBT((String) delta[0]);
				SkStuff.getNMSMethods().addToCompound(itemNBT, parsedNBT);
				ItemStack newItem = SkStuff.getNMSMethods().getItemWithNBT((ItemStack) tar, itemNBT);
				Object slot = target.getSource().getSingle(e);
				if (slot instanceof Slot) {
					((Slot) slot).setItem(newItem);
				}
			} else if (mode == ChangeMode.REMOVE) {
				String[] toRemove = Arrays.copyOf(delta, delta.length, String[].class);
				SkStuff.getNMSMethods().removeFromCompound(itemNBT, toRemove);
				ItemStack newItem = SkStuff.getNMSMethods().getItemWithNBT((ItemStack) tar, itemNBT);
				Object slot = target.getSource().getSingle(e);
				if (slot instanceof Slot) {
					((Slot) slot).setItem(newItem);
				}
			} else if (mode == ChangeMode.DELETE || mode == ChangeMode.RESET) {
				ItemStack newItem = SkStuff.getNMSMethods().getItemWithNBT((ItemStack) tar, null);
				Object slot = target.getSource().getSingle(e);
				if (slot instanceof Slot) {
					((Slot) slot).setItem(newItem);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<?>[] acceptChange(final ChangeMode mode) {
		if (mode == ChangeMode.ADD || mode == ChangeMode.REMOVE || mode == ChangeMode.DELETE || mode == ChangeMode.RESET) {
			return CollectionUtils.array(String[].class);
		}
		return null;
	}
}