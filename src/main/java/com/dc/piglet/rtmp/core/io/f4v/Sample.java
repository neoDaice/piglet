
package com.dc.piglet.rtmp.core.io.f4v;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
public class Sample implements Comparable {

    private static final Logger log = LoggerFactory.getLogger(Sample.class);
    private Chunk chunk;
    private int size;
    private int duration;
    private int time;
    private int compositionTimeOffset;
    private boolean syncSample;
    private long fileOffset;

    public int convertFromTimeScale(final long time) {
        final BigDecimal factor = new BigDecimal(time * 1000);
        return factor.divide(chunk.getTimeScale(), RoundingMode.HALF_EVEN).intValue();
    }

    public boolean isVideo() {
        return chunk.getSampleType().isVideo();
    }

    public long getFileOffset() {
        return fileOffset;
    }

    public void setFileOffset(long fileOffset) {
        this.fileOffset = fileOffset;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setSyncSample(boolean syncSample) {
        this.syncSample = syncSample;
    }

    public boolean isSyncSample() {
        return syncSample;
    }

    public int getCompositionTimeOffset() {
        return compositionTimeOffset;
    }

    public void setCompositionTimeOffset(int compositionTimeOffset) {
        this.compositionTimeOffset = compositionTimeOffset;
    }

    @Override
    public int compareTo(final Object o) {
        return time - ((Sample) o).time;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Sample)) {
            return false;
        }
        final Sample s = (Sample) o;
        return time == s.time;
    }

    @Override
    public int hashCode() {
        return time;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(chunk.getSampleType());
        if (syncSample) {
            sb.append(" (*sync*)");
        }
        sb.append(" fileOffset: ").append(fileOffset);
        sb.append(" size: ").append(size);
        sb.append(" duration: ").append(duration);
        sb.append(" time: ").append(time);
        if (compositionTimeOffset > 0) {
            sb.append(" c-time: ").append(compositionTimeOffset);
        }
        sb.append("]");
        return sb.toString();
    }
    
}
