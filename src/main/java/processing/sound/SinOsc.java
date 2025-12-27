package processing.sound;

import com.jsyn.unitgen.SineOscillator;


/**
 * This is a simple Sine Wave Oscillator.
 *
 * @webref Oscillators:SinOsc
 * @webBrief This is a simple Sine Wave Oscillator.
 **/
public class SinOsc extends Oscillator<SineOscillator> {

	/**
	 */
	public SinOsc() {
		super(new SineOscillator());
	}

}
