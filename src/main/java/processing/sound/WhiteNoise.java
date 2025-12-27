package processing.sound;


/**
 * This is a White Noise Generator. White Noise has a flat spectrum. 
 * @webref Noise:WhiteNoise
 * @webBrief This is a White Noise Generator.
 **/
public class WhiteNoise extends Noise<com.jsyn.unitgen.WhiteNoise> {

	/**
	 */
	public WhiteNoise() {
		super(new com.jsyn.unitgen.WhiteNoise());
		this.amplitude = this.noise.amplitude;
	}

	/**
	 * {@inheritDoc}
	 */
	public void amp(float amp) {
		// the JSyn Brownian noise generator can drift to exceed one, so tone down the volume a bit
		super.amp(amp / 4);
	}
}
