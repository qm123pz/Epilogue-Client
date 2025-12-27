package processing.sound.special;

import com.jsyn.data.FloatSample;

/**
 * @author IzumiiKonata
 * Date: 2025/4/10 19:17
 */
public class MultipleFloatSample extends FloatSample {

    public MultipleFloatSample(FloatSample... samples) {

        int channelsPerFrame = samples[0].getChannelsPerFrame();
        double frameRate = samples[0].getFrameRate();
        double pitch = samples[0].getPitch();

        for (FloatSample sample : samples) {
            if (/*sample.getNumFrames() != numFrames || */sample.getChannelsPerFrame() != channelsPerFrame || sample.getFrameRate() != frameRate || sample.getPitch() != pitch) {
                throw new RuntimeException("Fuck you");
            }
        }

        int numFramesMax = -1;
        int channelsPerFrameMax = -1;

        for (int i = 0; i < samples.length; i++) {
            FloatSample sample = samples[i];

            numFramesMax = Math.max(numFramesMax, sample.getNumFrames());
            channelsPerFrameMax = Math.max(channelsPerFrameMax, sample.getChannelsPerFrame());
        }

        allocate(numFramesMax, channelsPerFrameMax);
        setFrameRate(frameRate);
        setPitch(pitch);

        for (FloatSample sample : samples) {
            for (int i = 0; i < sample.getBuffer().length; i++) {
                this.getBuffer()[i] += sample.getBuffer()[i];
            }
        }

        System.out.println("Load OK!");
    }
}
