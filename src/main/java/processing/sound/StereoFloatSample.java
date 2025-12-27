package processing.sound;

import com.jsyn.data.FloatSample;

/**
 * @author IzumiiKonata
 * Date: 2024/12/21 17:12
 */
public class StereoFloatSample extends FloatSample {

    FloatSample[] buffers;

    public StereoFloatSample(int numFrames) {
        this(numFrames, 2);
    }

    StereoFloatSample(int numFrames, int channelsPerFrame) {
        allocate0(numFrames, channelsPerFrame);
    }

    private void allocate0(int numFrames, int channelsPerFrame) {

        buffers = new FloatSample[channelsPerFrame];

        for (int i = 0; i < channelsPerFrame; i++) {
            buffers[i] = new FloatSample(numFrames);
        }

    }

    public void writeTo(int to, int index, double value) {
        buffers[to].writeDouble(index, value);
    }

    public FloatSample getLeft() {
        return buffers[0];
    }

    public FloatSample getRight() {
        return buffers[1];
    }

    @Override
    public int getChannelsPerFrame() {
        return 2;
    }
}
