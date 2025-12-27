package processing.sound;


/**
* This is a pink noise generator. Pink Noise has a decrease of 3dB per octave.
* @webref Noise:PinkNoise
* @webBrief This is a pink noise generator.
**/
public class PinkNoise extends Noise<com.jsyn.unitgen.PinkNoise> {

	/**
	 */
	public PinkNoise() {
		super(new com.jsyn.unitgen.PinkNoise());
		this.amplitude = this.noise.amplitude;
	}

}
