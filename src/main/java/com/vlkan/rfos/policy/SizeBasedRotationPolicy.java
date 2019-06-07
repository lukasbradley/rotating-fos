package com.vlkan.rfos.policy;

import com.vlkan.rfos.Rotatable;
import com.vlkan.rfos.RotationConfig;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.TimerTask;

public class SizeBasedRotationPolicy implements RotationPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SizeBasedRotationPolicy.class);

    private final long checkIntervalMillis;

    private final long maxByteCount;

    private Rotatable rotatable;

    public SizeBasedRotationPolicy(long checkIntervalMillis, long maxByteCount) {

        if (checkIntervalMillis < 0) {
            String message = String.format("invalid interval {checkIntervalMillis=%d}", checkIntervalMillis);
            throw new IllegalArgumentException(message);
        }
        this.checkIntervalMillis = checkIntervalMillis;

        if (maxByteCount < 1) {
            String message = String.format("invalid size {maxByteCount=%d}", maxByteCount);
            throw new IllegalArgumentException(message);
        }
        this.maxByteCount = maxByteCount;

    }

    public long getCheckIntervalMillis() {
        return checkIntervalMillis;
    }

    public long getMaxByteCount() {
        return maxByteCount;
    }

    @Override
    public boolean isWriteSensitive() {
        return checkIntervalMillis == 0;
    }

    @Override
    public void acceptWrite(long byteCount) {
        if (byteCount > maxByteCount) {
            LocalDateTime now = rotatable.getConfig().getClock().now();
            rotate(now, byteCount, rotatable);
        }
    }

    @Override
    public void start(Rotatable rotatable) {
        this.rotatable = rotatable;
        if (checkIntervalMillis > 0) {
            TimerTask timerTask = createTimerTask(rotatable);
            rotatable.getConfig().getTimer().schedule(timerTask, 0, checkIntervalMillis);
        }
    }

    private TimerTask createTimerTask(final Rotatable rotatable) {
        final RotationConfig config = rotatable.getConfig();
        return new TimerTask() {
            @Override
            public void run() {

                // Get file size.
                LocalDateTime now = config.getClock().now();
                File file = config.getFile();
                long byteCount;
                try {
                    byteCount = file.length();
                } catch (Exception error) {
                    String message = String.format("failed accessing file size {file=%s}", file);
                    Exception extendedError = new IOException(message, error);
                    config.getCallback().onFailure(SizeBasedRotationPolicy.this, now, file, extendedError);
                    return;
                }

                // Rotate if necessary.
                if (byteCount > maxByteCount) {
                    rotate(now, byteCount, rotatable);
                }

            }
        };
    }

    private void rotate(LocalDateTime now, long byteCount, Rotatable rotatable) {
        LOGGER.debug("triggering {byteCount={}}", byteCount);
        rotatable.getConfig().getCallback().onTrigger(SizeBasedRotationPolicy.this, now);
        rotatable.rotate(SizeBasedRotationPolicy.this, now);
    }

    @Override
    public boolean equals(Object instance) {
        if (this == instance) return true;
        if (instance == null || getClass() != instance.getClass()) return false;
        SizeBasedRotationPolicy that = (SizeBasedRotationPolicy) instance;
        return checkIntervalMillis == that.checkIntervalMillis && maxByteCount == that.maxByteCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkIntervalMillis, maxByteCount);
    }

    @Override
    public String toString() {
        return String.format(
                "SizeBasedRotationPolicy{checkIntervalMillis=%d, maxByteCount=%d}",
                checkIntervalMillis, maxByteCount);
    }

}
