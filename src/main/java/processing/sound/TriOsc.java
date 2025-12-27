package processing.sound;

import com.jsyn.unitgen.TriangleOscillator;


/**
 * This is a simple triangle wave oscillator.
 * @webref Oscillators:TriOsc
 * @webBrief This is a simple triangle wave oscillator.
 **/
public class TriOsc extends Oscillator<TriangleOscillator> {

	/**
	 */
	public TriOsc() {
		super(new TriangleOscillator());
	}

}
