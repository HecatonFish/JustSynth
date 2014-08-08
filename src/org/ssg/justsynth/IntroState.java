package org.ssg.justsynth
;
import org.craigl.softsynth.SamplePlayer;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;

public class IntroState extends BasicGameState{

	public int stateID;

	SamplePlayer player;
	Image top, bottom;
	Color randColor;
	
	public IntroState(int i, SamplePlayer p, Color c){
		super();
		player = p;
		stateID = i;
		
		randColor = c;
	}

	public void init(GameContainer gc, StateBasedGame sbg) throws SlickException {
		top =((JustSynth)sbg).introImg1;
		bottom = ((JustSynth)sbg).introImg2;
	}

	@Override
	public void enter(GameContainer gc, StateBasedGame sbg) throws SlickException{
		
	}
	
	public void render(GameContainer gc, StateBasedGame sbg, Graphics g) throws SlickException {
		g.drawImage(top,0,0, randColor);
		g.drawImage(bottom,0,0);
	}

	public void update(GameContainer gc, StateBasedGame sbg, int delta) throws SlickException {
		Input input = gc.getInput();
		
		if(input.isKeyPressed(Input.KEY_ESCAPE)){
			player.stopPlayer();
			try {Thread.sleep(100);} catch (InterruptedException e) {}
    		gc.exit();
    	}else if(input.isKeyPressed(Input.KEY_ENTER)){
    		((SynthState) sbg.getState(JustSynth.SYNTHSTATE)).setBlack(false);
    		sbg.enterState(JustSynth.SYNTHSTATE);
    	}

	}
	
	@Override
	public int getID() {
		return stateID;
	}
	
}
