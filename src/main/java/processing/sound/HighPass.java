package processing.sound;

import com.jsyn.unitgen.FilterHighPass;


/**
 * This is a high pass filter.
 * @webref Effects:HighPass
 * @webBrief This is a high pass filter.
 * @param parent PApplet: typically use "this"
 **/
public class HighPass extends Filter<FilterHighPass> {

	public HighPass() {
		super();
	}

	@Override
	protected FilterHighPass newInstance() {
		return new FilterHighPass();
	}
}
