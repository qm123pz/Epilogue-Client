package processing.sound;

import com.jsyn.unitgen.SquareOscillator;


/**
 * This is a simple Square Wave Oscillator.
 * @webref Oscillators:SqrOsc
 * @webBrief This is a simple Square Wave Oscillator.
 **/
public class SqrOsc extends Oscillator<SquareOscillator> {

	/**
	 */
	public SqrOsc() {
		super(new SquareOscillator());
	}

}
