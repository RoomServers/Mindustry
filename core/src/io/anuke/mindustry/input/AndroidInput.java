package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

public class AndroidInput extends InputHandler{
	public float lmousex, lmousey;
	public float mousex, mousey;
	public boolean brokeBlock = false;
	private boolean placing = true;
	private float warmup;
	private float warmupDelay = 20;
	
	public AndroidInput(){
		Inputs.addProcessor(new GestureDetector(20, 0.5f, 2, 0.15f, new GestureHandler(this)));
	}
	
	@Override public float getCursorEndX(){ return Gdx.input.getX(0); }
	@Override public float getCursorEndY(){ return Gdx.input.getY(0); }
	@Override public float getCursorX(){ return mousex; }
	@Override public float getCursorY(){ return mousey; }
	@Override public boolean drawPlace(){ return placing; }

	@Override
	public boolean keyDown(int keycode){
		if(keycode == Keys.E){
			
		}
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button){
		brokeBlock = false;
		if(placing && pointer == 0 && !player.placeMode.pan){
			player.placeMode.released(getBlockX(), getBlockY(), getBlockEndX(), getBlockEndY());
			placing = false;
		}
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button){
		if(ui.hasMouse()) return false;
		
		ui.hideTooltip();
		lmousex = screenX;
		lmousey = screenY;
		
		if(!player.placeMode.pan){
			if(pointer == 0){
				placing = true;
			
				mousex = screenX;
				mousey = screenY;
			
			}else{
				placing = false;
			}
		}
		
		warmup = 0;

		if(!GameState.is(State.menu)){
			Tile cursor = world.tile(Mathf.scl2(Graphics.mouseWorld().x, tilesize), Mathf.scl2(Graphics.mouseWorld().y, tilesize));
			if(cursor != null && !ui.hasMouse()){
				Tile linked = cursor.isLinked() ? cursor.getLinked() : cursor;
				if(linked != null && linked.block() instanceof Configurable){
					ui.showConfig(linked);
				}else if(!ui.hasConfigMouse()){
					ui.hideConfig();
				}
			}
		}
		return false;
	}
	
	@Override
	public void resetCursor(){
		mousex = Gdx.graphics.getWidth()/2;
		mousey = Gdx.graphics.getHeight()/2;
	}
	
	@Override
	public boolean cursorNear(){
		return true;
	}

	public Tile selected(){
		Vector2 vec = Graphics.world(mousex, mousey);
		return world.tile(Mathf.scl2(vec.x, tilesize), Mathf.scl2(vec.y, tilesize));
	}

	public void breakBlock(){
		Tile tile = selected();
		player.breaktime += Timers.delta();

		if(player.breaktime >= tile.block().breaktime){
			brokeBlock = true;
			breakBlock(tile.x, tile.y);
			player.breaktime = 0f;
		}
	}
	
	public void tryPlaceBlock(int tilex, int tiley){
		if(player.recipe != null && control.hasItems(player.recipe.requirements) && validPlace(tilex, tiley, player.recipe.result)){

			placeBlock(tilex, tiley, player.recipe.result, player.rotation, true);

			for(ItemStack stack : player.recipe.requirements){
				control.removeItem(stack);
			}
		}
	}

	@Override
	public void update(){

		if(Gdx.input.isTouched(0) && Mathf.near2d(lmousex, lmousey, Gdx.input.getX(0), Gdx.input.getY(0), Unit.dp.inPixels(50))
				&& !ui.hasMouse()){
			warmup += Timers.delta();

			float lx = mousex, ly = mousey;

			mousex = Gdx.input.getX(0);
			mousey = Gdx.input.getY(0);

			Tile sel = selected();

			if(sel == null)
				return;

			if(warmup > warmupDelay && validBreak(sel.x, sel.y)){
				player.breaktime += Timers.delta();

				if(player.breaktime > selected().block().breaktime){
					breakBlock();
					player.breaktime = 0;
				}
			}

			mousex = lx;
			mousey = ly;
		}else{
			warmup = 0;
			player.breaktime = 0;

			mousex = Mathf.clamp(mousex, 0, Gdx.graphics.getWidth());
			mousey = Mathf.clamp(mousey, 0, Gdx.graphics.getHeight());
		}
	}

	public int touches(){
		int sum = 0;
		for(int i = 0; i < 10; i++){
			if(Gdx.input.isTouched(i))
				sum++;
		}
		return sum;
	}
}
