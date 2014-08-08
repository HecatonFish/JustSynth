package org.ssg.justsynth;

import org.craigl.softsynth.BasicOscillator;
import org.craigl.softsynth.SamplePlayer;
import org.craigl.softsynth.SampleProviderIntfc;

public class Mixer implements SampleProviderIntfc{

	private SampleProviderIntfc provider;
	
	public Mixer(){
		
	}

	public void setSampleProvider(SampleProviderIntfc provider){
		this.provider = provider;
	}
	
	//Goes through the individual waves calculated for each note and adds them together
	//The summed value is stored into the last row of the buffer
	//tempInt is unused in this particular method
	public int getSamples(byte[][] buffer, int tempInt) {
		
		provider.getSamples(buffer, -1);

		//Go through each sample (time step)
		for(int i=0; i<buffer[0].length-1; i+= 2){
			buffer[BasicOscillator.NUM_NOTES][i] = 0;
			//Go through each note at that time step
			short sum = 0;
			byte b2;
			byte b1;
			short addend;
			for(int j=0; j<buffer.length-1; j++){
				//Add the samples
				b2 = buffer[j][i];
				b1 = buffer[j][i+1];
				addend = (short)(  ((b2&0xFF)<<8) | (b1&0xFF) );

				sum += addend;
			}
			
			buffer[BasicOscillator.NUM_NOTES][i] = (byte)(sum >> 8);
			buffer[BasicOscillator.NUM_NOTES][i+1] = (byte)(sum & 0xFF);
			
			//printBuffer(buffer, i);
		}
		
		return SamplePlayer.BUFFER_SIZE;
	}
	
	public void printBufferList(byte[][] buffer, int i){
		if(i%2 == 0){
			byte b2 = buffer[BasicOscillator.NUM_NOTES][i];
			byte b1 = buffer[BasicOscillator.NUM_NOTES][i+1];
			short s=(short)( ((b2&0xFF)<<8) | (b1&0xFF) );
			System.out.println(s);
		}
	}
}
