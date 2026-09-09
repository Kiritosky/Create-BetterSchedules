package net.jxstanix.betterschedules.train;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * Drives a train a precise distance while it is parked at a station.
 *
 * <p>Create only decays {@link Train#speed} when {@link Train#manualTick} is false, and applies
 * {@code speed} verbatim as the distance travelled this tick, so setting both every tick moves the
 * train by an exact amount without ever calling {@link Train#leaveStation()} - the train stays
 * docked and its schedule keeps running.
 */
public final class TrainShunter {

	/** Blocks per tick while cruising. Stays under the 0.25 threshold that suspends portable interfaces. */
	private static final double CRUISE_SPEED = 0.06;
	private static final double MIN_SPEED = 0.008;
	private static final double EPSILON = 1e-4;

	/** Ticks of zero movement before a shunt gives up. */
	private static final int STUCK_LIMIT = 100;

	private static final String REMAINING = "BsShuntRemaining";
	private static final String STEP = "BsShuntStep";
	private static final String MOVING = "BsShuntMoving";
	private static final String STUCK = "BsShuntStuck";
	private static final String TICK = "BsShuntTick";

	public enum Result {
		MOVING, DONE, BLOCKED
	}

	private TrainShunter() {}

	/**
	 * Advances the shunt by one tick. Call every tick with the same context tag until the result is
	 * no longer {@link Result#MOVING}.
	 */
	public static Result tick(Level level, Train train, CompoundTag context, double distance, boolean forward) {
		if (distance <= EPSILON)
			return Result.DONE;

		// ScheduleRuntime#tick re-enters within the same game tick when a train runs out of track
		long gameTime = level.getGameTime();
		if (context.contains(TICK) && context.getLong(TICK) == gameTime)
			return Result.MOVING;

		double remaining = context.contains(REMAINING) ? context.getDouble(REMAINING) : distance;
		int stuck = context.getInt(STUCK);

		if (context.getBoolean(MOVING)) {
			if (Math.abs(train.speed) < 1e-6) {
				stuck++;
			} else {
				remaining -= context.getDouble(STEP);
				stuck = 0;
			}
		}

		if (train.derailed || stuck > STUCK_LIMIT) {
			stop(train, context);
			return Result.BLOCKED;
		}

		if (remaining <= EPSILON) {
			stop(train, context);
			return Result.DONE;
		}

		double step = Math.min(Math.min(CRUISE_SPEED, remaining), Math.max(MIN_SPEED, remaining * 0.4));
		train.speed = forward ? step : -step;
		train.manualTick = true;

		context.putDouble(REMAINING, remaining);
		context.putDouble(STEP, step);
		context.putBoolean(MOVING, true);
		context.putInt(STUCK, stuck);
		context.putLong(TICK, gameTime);
		return Result.MOVING;
	}

	/** How far this shunt still has to travel, for status text. */
	public static double remaining(CompoundTag context, double fallback) {
		return context.contains(REMAINING) ? context.getDouble(REMAINING) : fallback;
	}

	public static void stop(Train train, CompoundTag context) {
		train.speed = 0;
		context.remove(REMAINING);
		context.remove(STEP);
		context.remove(MOVING);
		context.remove(STUCK);
		context.remove(TICK);
	}

	public static boolean inProgress(CompoundTag context) {
		return context.contains(REMAINING);
	}

	/**
	 * Distance between the centres of carriage {@code index} and the one behind it. Moving the train
	 * forward by this much puts the following carriage where the current one stood.
	 */
	public static double carriagePitch(Train train, int index) {
		if (index < 0 || index + 1 >= train.carriages.size())
			return 0;
		Carriage current = train.carriages.get(index);
		Carriage next = train.carriages.get(index + 1);
		double gap = index < train.carriageSpacing.size() ? train.carriageSpacing.get(index) : 0;
		return current.bogeySpacing / 2.0 + gap + next.bogeySpacing / 2.0;
	}

	/** Length of a single carriage, used as the unit for manual shunts. */
	public static double averageCarriageLength(Train train) {
		if (train.carriages.isEmpty())
			return 0;
		return (double) train.getTotalLength() / train.carriages.size();
	}
}
