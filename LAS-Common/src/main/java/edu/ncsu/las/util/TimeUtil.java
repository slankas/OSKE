package edu.ncsu.las.util;

import static java.lang.System.nanoTime;

public class TimeUtil {
	/**
	 * Nanos per millisecond for conversions to units that humans find easier to
	 * understand.
	 */
	public static final double NANOS_PER_MILLISECOND = 1000000.0;

	/**
	 * Get elapsed time in milliseconds.
	 *
	 * @return Elapsed time in milliseconds.
	 */
	public static double getElapsedMills(long startTimeInNanos) {
		return getElapsedNanos(startTimeInNanos) / NANOS_PER_MILLISECOND;
	}

	/**
	 * Get elapsed time in nanos.
	 *
	 * @return Elapsed time in nanos.
	 */
	public static long getElapsedNanos(long startTimeInNanos) {
		startTimeInNanos = nanoTime() - startTimeInNanos;
		return startTimeInNanos == 0 ? 1 : startTimeInNanos;
	}
}
