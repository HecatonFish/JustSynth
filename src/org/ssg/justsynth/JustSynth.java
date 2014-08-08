package org.ssg.justsynth;

import org.craigl.softsynth.*;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;


public class JustSynth extends StateBasedGame {

	public static final int INTROSTATE = 00;
	public static final int SYNTHSTATE = 10;
	
	static BasicOscillator osc;
	static VCA vca;
	static SamplePlayer player;
	static Mixer mixer;
	
	Image introImg1, introImg2; 
	
	public JustSynth() throws SlickException {
		super("JustSynthThyme");
		
		Color randColor = new Color((int)(Math.random()*256),(int)( Math.random()*256), (int)(Math.random()*256));
		
		//The first state to be added is entered by default
		this.addState(new IntroState(INTROSTATE, player, randColor));
		this.addState(new SynthState(SYNTHSTATE, osc, vca, player, randColor));
	}

	public void initStatesList(GameContainer gc) throws SlickException {
		introImg1 = new Image("resources/1.png");
		introImg2 = new Image("resources/2.png");
	}

	public static void main(String[] args) throws SlickException {
	
		// Create an oscillator
		osc = new BasicOscillator();

		// Create a VCA
		vca = new VCA();
		osc.setVCA(vca);
		
		// Set the VCA's sample provider
		vca.setSampleProvider(osc);

		// Parameterize the envelope generator
		vca.setAttackTimeInMS(150);
		vca.setDecayTimeInMS(80);
		vca.setSustainLevel(0.4);
		vca.setReleaseTimeInMS(400);
		
		mixer = new Mixer();
		mixer.setSampleProvider(vca);
		
		// Create a sample player
		player = new SamplePlayer();
		
		// Sets the sample player's sample provider
		player.setSampleProvider(mixer);
		
		// Start the player
		player.startPlayer();	
		
		AppGameContainer app = new AppGameContainer(new JustSynth());
		app.setDisplayMode(1000, 700, false);
		//app.setVSync(true);
		//app.setTargetFrameRate(60);
		app.setAlwaysRender(true);
		app.setShowFPS(false);
		app.setTitle("Just Synth Thyme");
//		app.setSmoothDeltas(true);
//		if (app.supportsMultiSample())
//			app.setMultiSample(2);
		app.setFullscreen(false);
		//app.setMaximumLogicUpdateInterval(24);
		//app.setMinimumLogicUpdateInterval(24);
		app.start();
	}

}
