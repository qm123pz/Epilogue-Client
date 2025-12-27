package processing.sound;

import com.jsyn.ports.UnitOutputPort;


/**
 * Common superclass of all audio analysis classes
 * @webref Analysis
 */
public abstract class Analyzer {

	protected SoundObject input;

	protected Analyzer() {
		Engine.getEngine();
	}

	/**
	 * Define the audio input for the analyzer.
	 * 
	 * @param input The input processing.sound source
	 * @webref Analysis:Analyzer
	 **/
	public void input(SoundObject input) {
		if (this.input == input) {
//			Engine.printWarning("This input was already connected to the analyzer");
		} else {
			if (this.input != null) {
				if (!this.input.isPlaying()) {
					// unit was only analyzed but not playing out loud - remove from synth
					Engine.getEngine().remove(this.input.circuit);
				}

				this.removeInput();
			}

			this.input = input;
			if (!this.input.isPlaying()) {
				Engine.getEngine().add(input.circuit);
			}

			this.setInput(input.circuit.output.output);
		}
	}

	// remove the current input
	public abstract void removeInput();

	// connect processing.sound source in subclass AND add analyser unit to Engine
	protected abstract void setInput(UnitOutputPort input);
}
