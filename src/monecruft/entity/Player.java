package monecruft.entity;

import org.lwjgl.input.Mouse;

import monecruft.blocks.Block;
import monecruft.blocks.BlockLibrary;
import monecruft.gui.Chunk;
import monecruft.gui.GlobalTextManager;
import monecruft.gui.WorldFacade;
import monecruft.utils.InputHandler;
import monecruft.utils.KeyToggleListener;
import monecruft.utils.KeyValueListener;
import monecruft.utils.VoxelUtils;
import ivengine.view.Camera;

public class Player implements KeyToggleListener, KeyValueListener
{
	//private static final float DEFAULT_SPEED=8;
	private static final float DEFAULT_SPEED=8;
	private static final float FLY_SPEED=100;
	private static final float DEFAULT_JUMPSPEED=7;
	private static final float FLY_JUMPSPEED=20;
	private static final float DEFAULT_HEIGHT=1.8f;
	private static final float DEFAULT_HEIGHT_APROX=1.79f;
	private static final float EYE_POS=1.65f;
	private static final float DEFAULT_SIZE=0.4f;
	private static final float DEFAULT_SIZE_APROX=0.39f;
	private static final int DEFAULT_MOUSE_SENSIVITY=100;
	private static final float PIMEDIOS=(float)(Math.PI/2);
	private static final float SQRT2=(float)Math.sqrt(2);
	private static final float MAX_RAYCAST_DISTANCE=5;
	private static final int DEFAULT_SELECTED_BLOCK=2;
	
	private Camera cam;
	private float xpos,ypos,zpos;
	private float pitch,yaw;
	private float yvel=0;
	private float xvel=0;
	private float zvel=0;
	private float currentSpeed=DEFAULT_SPEED;
	private boolean flying=false;
	private boolean action_deleteNextBlock=false;
	private boolean action_createNextBlock=false;
	private boolean grounded=false;
	
	private int selectedBlock=DEFAULT_SELECTED_BLOCK;
	
	
	public Player(float ix,float iy,float iz,Camera cam)
	{
		this.cam=cam;
		this.xpos=ix;this.ypos=iy;this.zpos=iz;
		this.pitch=0;this.yaw=0;
		InputHandler.addKeyToggleListener(InputHandler.SHIFT_VALUE,this);
		InputHandler.addKeyToggleListener(InputHandler.MOUSE_BUTTON1_VALUE,this);
		InputHandler.addKeyToggleListener(InputHandler.MOUSE_BUTTON2_VALUE,this);
		InputHandler.addKeyValueListener(InputHandler.MOUSE_WHEEL_VALUE, this);
	}
	public void update(float tEl,WorldFacade wf)
	{
		handleEvents(wf);
		//System.out.println(this.xpos+" "+this.ypos+" "+this.zpos);
		float yAxisPressed=InputHandler.isWPressed()||InputHandler.isSPressed()?SQRT2:1;
		float xAxisPressed=InputHandler.isAPressed()||InputHandler.isDPressed()?SQRT2:1;
		boolean underwater=isUnderwater(wf,EYE_POS/2);
		currentSpeed=flying?FLY_SPEED:underwater?DEFAULT_SPEED/2:DEFAULT_SPEED;
		if(InputHandler.isWPressed()) moveForward(currentSpeed*tEl/xAxisPressed,wf);
		if(InputHandler.isAPressed()) moveLateral(-currentSpeed*tEl/yAxisPressed,wf);
		if(InputHandler.isSPressed()) moveForward(-currentSpeed*tEl/xAxisPressed,wf);
		if(InputHandler.isDPressed()) moveLateral(currentSpeed*tEl/yAxisPressed,wf);
		this.addPitch(-(float)(Mouse.getDY())/DEFAULT_MOUSE_SENSIVITY);
		this.addYaw((float)(Mouse.getDX())/DEFAULT_MOUSE_SENSIVITY);
		//Gravity
		if(!grounded) {
			if(underwater){
				this.yvel-=wf.getWorldGravity()*tEl/2;
				if(this.yvel<-wf.getWorldGravity()/2) this.yvel=-wf.getWorldGravity()/2;
			}
			else this.yvel-=wf.getWorldGravity()*tEl;
		}
		this.moveY(this.ypos+(yvel*tEl),wf);
		//Jump (Calc in next loop)
		if(InputHandler.isSPACEPressed()) 
		{
			if(this.grounded){
				this.yvel=DEFAULT_JUMPSPEED;
				this.grounded=false;
			}
			else if(underwater){
				this.yvel=isUnderwater(wf)?DEFAULT_JUMPSPEED/2:DEFAULT_JUMPSPEED;
				this.grounded=false;
			}
			if(flying) this.yvel=FLY_JUMPSPEED;
		}
		//yvel*=wf.getWorldAirFriction();
		//if(wf.getContent(this.xpos, this.ypos, this.zpos)!=0) {this.ypos=(int)(ypos)+1;this.yvel=0;}
		//else if(wf.getContent(this.xpos, this.ypos+DEFAULT_HEIGHT, this.zpos)!=0){this.ypos=(int)(ypos+DEFAULT_HEIGHT)-DEFAULT_HEIGHT;this.yvel=0;}
		updateCamera();
		//if(InputHandler.isSHIFTPressed()) this.cam.moveUp(-1f);
		wf.reloadPlayerFOV((int)Math.floor(this.getX()/Chunk.CHUNK_DIMENSION), 0, (int)Math.floor(this.getZ()/Chunk.CHUNK_DIMENSION));
		
		//Raycasting
		//RaycastResult res=raycast(this.pitch,this.yaw,this.xpos,this.ypos,this.zpos,wf,MAX_RAYCAST_DISTANCE);
		
		
	}
	public boolean isUnderwater(WorldFacade wf)
	{
		return isUnderwater(wf,EYE_POS);
	}
	public boolean isUnderwater(WorldFacade wf,float yoffset)
	{
		return BlockLibrary.isLiquid(wf.getContent(this.xpos, this.ypos+yoffset, this.zpos));
	}
	public float getAverageLightExposed(WorldFacade wf)
	{
		float zw=(float)(this.zpos-(Math.floor(this.zpos)));
		if(zw>0.5){
			return (1.5f-zw)*getAverageLightInX(wf,xpos,ypos+EYE_POS,zpos) + (zw-0.5f)*getAverageLightInX(wf,xpos,ypos+EYE_POS,zpos+1);
		}
		else{
			return (zw+0.5f)*getAverageLightInX(wf,xpos,ypos+EYE_POS,zpos) + (0.5f-zw)*getAverageLightInX(wf,xpos,ypos+EYE_POS,zpos-1);
		}
	}
	private float getAverageLightInX(WorldFacade wf,float xpos,float ypos,float zpos)
	{
		float xw=(float)(this.xpos-(Math.floor(this.xpos)));
		if(xw>0.5){
			return (1.5f-xw)*getAverageLightInY(wf,xpos,ypos+EYE_POS,zpos) + (xw-0.5f)*getAverageLightInY(wf,xpos+1,ypos+EYE_POS,zpos);
		}
		else{
			return (xw+0.5f)*getAverageLightInY(wf,xpos,ypos+EYE_POS,zpos) + (0.5f-xw)*getAverageLightInY(wf,xpos-1,ypos+EYE_POS,zpos);
		}
	}
	private float getAverageLightInY(WorldFacade wf,float xpos,float ypos,float zpos)
	{
		float yw=(float)(ypos-(Math.floor(ypos)));
		if(yw>0.5){
			return (1.5f-yw)*wf.getContentMaxLight(xpos, ypos, zpos) + (yw-0.5f)*wf.getContentMaxLight(xpos, ypos+1, zpos);
		}
		else{
			return (yw+0.5f)*wf.getContentMaxLight(xpos, ypos, zpos) + (0.5f-yw)*wf.getContentMaxLight(xpos, ypos-1, zpos);
		}
	}
	private void handleEvents(WorldFacade wf)
	{
		if(action_deleteNextBlock)
		{
			action_deleteNextBlock=false;
			//Chunk c=wf.getChunkByIndex((int)(this.xpos)/Chunk.CHUNK_DIMENSION, (int)(this.ypos-1)/Chunk.CHUNK_DIMENSION, (int)(this.zpos)/Chunk.CHUNK_DIMENSION);
			//if(c!=null) c.setCubeAt(VoxelUtils.trueMod(this.xpos,Chunk.CHUNK_DIMENSION), VoxelUtils.trueMod(this.ypos-1,Chunk.CHUNK_DIMENSION), VoxelUtils.trueMod(this.zpos,Chunk.CHUNK_DIMENSION),(byte)(0));
			RaycastResult res=raycast(this.pitch,this.yaw,this.xpos,this.ypos+EYE_POS,this.zpos,wf,MAX_RAYCAST_DISTANCE);
			if(res!=null)
			{
				Chunk c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
				System.out.println("CHUNK "+c.getX()+" "+c.getY()+" "+c.getZ());
				if(c!=null) c.setCubeAt(res.getX(), res.getY(), res.getZ(),(byte)(0));	
			}
		}
		if(action_createNextBlock)
		{
			action_createNextBlock=false;
			//Chunk c=wf.getChunkByIndex((int)(this.xpos)/Chunk.CHUNK_DIMENSION, (int)(this.ypos-1)/Chunk.CHUNK_DIMENSION, (int)(this.zpos)/Chunk.CHUNK_DIMENSION);
			//if(c!=null) c.setCubeAt(VoxelUtils.trueMod(this.xpos,Chunk.CHUNK_DIMENSION), VoxelUtils.trueMod(this.ypos-1,Chunk.CHUNK_DIMENSION), VoxelUtils.trueMod(this.zpos,Chunk.CHUNK_DIMENSION),(byte)(0));
			RaycastResult res=raycast(this.pitch,this.yaw,this.xpos,this.ypos+EYE_POS,this.zpos,wf,MAX_RAYCAST_DISTANCE);
			if(res!=null)
			{
				Chunk c=null;
				int fx=-1;
				int fy=-1;
				int fz=-1;
				switch(res.face)
				{
				case XP:
					if(res.getX()==Chunk.CHUNK_DIMENSION-1){
						c=wf.getChunkByIndex(res.getChunkX()+1, res.getChunkY(), res.getChunkZ());
						fx=0; fy=res.getY(); fz=res.getZ();
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX()+1; fy=res.getY(); fz=res.getZ();
					}
					break;
				case XM:
					if(res.getX()==0){
						c=wf.getChunkByIndex(res.getChunkX()-1, res.getChunkY(), res.getChunkZ());
						fx=Chunk.CHUNK_DIMENSION-1; fy=res.getY(); fz=res.getZ();
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX()-1; fy=res.getY(); fz=res.getZ();
					}
					break;
				case YP:
					if(res.getY()==Chunk.CHUNK_DIMENSION-1){
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY()+1, res.getChunkZ());
						fx=res.getX(); fy=0; fz=res.getZ();
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX(); fy=res.getY()+1; fz=res.getZ();
					}
					break;
				case YM:
					if(res.getY()==0){
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY()-1, res.getChunkZ());
						fx=res.getX(); fy=Chunk.CHUNK_DIMENSION-1; fz=res.getZ();
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX(); fy=res.getY()-1; fz=res.getZ();
					}
					break;
				case ZP:
					if(res.getZ()==Chunk.CHUNK_DIMENSION-1){
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ()+1);
						fx=res.getX(); fy=res.getY(); fz=0;
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX(); fy=res.getY(); fz=res.getZ()+1;
					}
					break;
				case ZM:
					if(res.getZ()==0){
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ()-1);
						fx=res.getX(); fy=res.getY(); fz=Chunk.CHUNK_DIMENSION-1;
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX(); fy=res.getY(); fz=res.getZ()-1;
					}
					break;
				}
				if(c!=null) c.setCubeAt(fx, fy, fz,(byte)(this.selectedBlock));	
			}
		}
	}
	protected void moveForward(float amount,WorldFacade wf)
	{
		moveX(this.xpos+(float)(Math.sin(this.yaw))*amount,wf);
		moveZ(this.zpos-(float)(Math.cos(this.yaw))*amount,wf);
	}
	protected void moveLateral(float amount,WorldFacade wf)
	{
		moveZ(this.zpos+(float)(Math.sin(this.yaw)*amount),wf);
		moveX(this.xpos+(float)(Math.cos(this.yaw)*amount),wf);
	}
	void addPitch(float amount)
	{
		this.pitch+=amount;
		if(this.pitch>PIMEDIOS) this.pitch=(PIMEDIOS);
		else if(this.pitch<-PIMEDIOS) this.pitch=(-PIMEDIOS);
	}
	private void addYaw(float amount)
	{
		this.yaw+=amount;
	}
	private void updateCamera()
	{
		this.cam.moveTo(this.xpos, this.ypos+EYE_POS, this.zpos);
		this.cam.setPitch(pitch);
		this.cam.setYaw(yaw);
	}
	private void moveX(float to,WorldFacade wf){
		this.grounded=false;
		float step=this.xpos;
		boolean end=false;
		if(to<step){
			while(!end){
				if(to<step-0.9) step=step-0.9f;
				else{
					step=to;
					end=true;
				}
				if(!stepX(step,wf)) break;
			}
		}else{
			while(!end){
				if(to>step+0.9) step=step+0.9f;
				else{
					step=to;
					end=true;
				}
				if(!stepX(step,wf)) break;
			}
		}
	}
	private void moveY(float to,WorldFacade wf){
		float step=this.ypos;
		boolean end=false;
		if(to<step){
			while(!end){
				if(to<step-0.9) step=step-0.9f;
				else{
					step=to;
					end=true;
				}
				if(!stepY(step,wf)) break;
			}
		}else{
			while(!end){
				if(to>step+0.9) step=step+0.9f;
				else{
					step=to;
					end=true;
				}
				if(!stepY(step,wf)) break;
			}
		}
	}
	private void moveZ(float to,WorldFacade wf){
		this.grounded=false;
		float step=this.zpos;
		boolean end=false;
		if(to<step){
			while(!end){
				if(to<step-0.9) step=step-0.9f;
				else{
					step=to;
					end=true;
				}
				if(!stepZ(step,wf)) break;
			}
		}else{
			while(!end){
				if(to>step+0.9) step=step+0.9f;
				else{
					step=to;
					end=true;
				}
				if(!stepZ(step,wf)) break;
			}
		}
	}
	private boolean stepX(float to,WorldFacade wf)
	{
		if(to<this.xpos)
		{
			if(!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos, this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos, this.zpos-DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX/2), this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX/2), this.zpos-DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX), this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX), this.zpos-DEFAULT_SIZE_APROX))) {this.xpos=to;return true;}
			else {this.xpos=(int)(Math.floor(to-DEFAULT_SIZE))+1+DEFAULT_SIZE;this.xvel=0;return false;}
		}else{
			if(!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos, this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos, this.zpos-DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX/2), this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX/2), this.zpos-DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX), this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX), this.zpos-DEFAULT_SIZE_APROX))) {this.xpos=to;return true;}
			else {this.xpos=(int)(Math.floor(to+DEFAULT_SIZE))-DEFAULT_SIZE; this.xvel=0;return false;}
		}
	}
	private boolean stepY(float to,WorldFacade wf)
	{
		this.grounded=false;
		if(to<this.ypos)
		{
			if(!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, to,this.zpos ))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos, to,this.zpos+DEFAULT_SIZE_APROX ))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, to,this.zpos ))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos, to,this.zpos-DEFAULT_SIZE_APROX ))) {this.ypos=to;return true;}
			else {this.ypos=(int)(Math.floor(to))+1;this.yvel=0;this.grounded=true;return false;}
		}else{
			if(!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, to+DEFAULT_HEIGHT, this.zpos))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos, to+DEFAULT_HEIGHT,this.zpos+DEFAULT_SIZE_APROX ))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, to+DEFAULT_HEIGHT,this.zpos ))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos, to+DEFAULT_HEIGHT,this.zpos-DEFAULT_SIZE_APROX ))) {this.ypos=to;return true;}
			else {this.ypos=(int)(Math.floor(to+DEFAULT_HEIGHT))-DEFAULT_HEIGHT;this.yvel=0;return false;}
		}
	}
	private boolean stepZ(float to,WorldFacade wf)
	{
		if(to<this.zpos)
		{
			if(!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos, to-DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos, to-DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX/2), to-DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX/2), to-DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX), to-DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX), to-DEFAULT_SIZE))) {this.zpos=to; return true;}
			else {this.zpos=(int)(Math.floor(to-DEFAULT_SIZE))+1+DEFAULT_SIZE;this.zvel=0;return false;}
		}else{
			if(!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos, to+DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos, to+DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX/2), to+DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX/2), to+DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX), to+DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX), to+DEFAULT_SIZE))) {this.zpos=to; return true;}
			else {this.zpos=(int)(Math.floor(to+DEFAULT_SIZE))-DEFAULT_SIZE;this.zvel=0;return false;}
		}
	}
	public float getX()
	{
		return this.xpos;
	}
	public float getY()
	{
		return this.ypos;
	}
	public float getZ()
	{
		return this.zpos;
	}
	@Override
	public void notifyKeyToggle(int code) {
		switch(code)
		{
		case InputHandler.SHIFT_VALUE:
			if(flying)
			{
				flying=false;
				this.currentSpeed=DEFAULT_SPEED;
			}
			else
			{
				flying=true;
				this.currentSpeed=FLY_SPEED;
			}
			break;
		case InputHandler.MOUSE_BUTTON2_VALUE:
			this.action_deleteNextBlock=true;
			break;
		case InputHandler.MOUSE_BUTTON1_VALUE:
			this.action_createNextBlock=true;
			break;
		}
		
	}
	@Override
	public void notifyKeyIncrement(int code, int value) {
		switch(code)
		{
		case InputHandler.MOUSE_WHEEL_VALUE:
			this.selectedBlock+=value;
			while(this.selectedBlock<0) this.selectedBlock+=BlockLibrary.size();
			while(this.selectedBlock>=BlockLibrary.size()) this.selectedBlock-=BlockLibrary.size();
			GlobalTextManager.insertText(BlockLibrary.getName((byte)this.selectedBlock)+"");
			break;
		}
	}
	
	public static RaycastResult raycast(float pitch,float yaw,float ix,float iy,float iz,WorldFacade wf,float maxdist)
	{
		double dy=-Math.sin(pitch);
		double subx=Math.cos(pitch);
		double dz=-Math.cos(yaw)*subx;
		double dx=Math.sin(yaw)*subx;
		System.out.println(dx+","+dy+","+dz);
		System.out.println(ix+" "+iy+" "+iz);
		double cx=ix;
		double cy=iy;
		double cz=iz;
		double nextx,nexty,nextz;
		double currentDist=0;
		Block.facecode currentFace;
		while(currentDist<maxdist)
		{
			if(dx>0){
				nextx=Math.ceil(cx)-cx;
				if(nextx==0) nextx=nextx+1;
			}
			else{
				nextx=Math.floor(cx)-cx;
				if(nextx==0) nextx=nextx-1;
			}
			if(dy>0){
				nexty=Math.ceil(cy)-cy;
				if(nexty==0) nexty=nexty+1;
			}
			else{
				nexty=Math.floor(cy)-cy;
				if(nexty==0) nexty=nexty-1;
			}
			if(dz>0){
				nextz=Math.ceil(cz)-cz;
				if(nextz==0) nextz=nextz+1;
			}
			else{
				nextz=Math.floor(cz)-cz;
				if(nextz==0) nextz=nextz-1;
			}
			double mulx,muly,mulz;
			mulx= nextx/dx;
			muly= nexty/dy;
			mulz= nextz/dz;
			double mul;
			if(mulx<muly&&mulx<mulz){
				mul=mulx;
				currentFace=dx>0?Block.facecode.XM : Block.facecode.XP;
			}
			else if(muly<mulx&&muly<mulz){
				mul=muly;
				currentFace=dy>0?Block.facecode.YM : Block.facecode.YP;
			}
			else{
				mul=mulz;
				currentFace=dz>0?Block.facecode.ZM : Block.facecode.ZP;
			}
			System.out.println(mul);
			mul+=0.001;
			cx=cx+(dx*mul);
			cy=cy+(dy*mul);
			cz=cz+(dz*mul);
			System.out.println(cx+","+cy+","+cz);
			Chunk c=wf.getChunkByIndex((int)Math.floor(cx/Chunk.CHUNK_DIMENSION), (int)Math.floor(cy/Chunk.CHUNK_DIMENSION), (int)Math.floor(cz/Chunk.CHUNK_DIMENSION));
			if(c!=null)
			{
				int cubex=VoxelUtils.trueMod((float)cx, Chunk.CHUNK_DIMENSION);
				int cubey=VoxelUtils.trueMod((float)cy, Chunk.CHUNK_DIMENSION);
				int cubez=VoxelUtils.trueMod((float)cz, Chunk.CHUNK_DIMENSION);
				byte cube=c.getCubeAt(cubex,cubey,cubez);
				System.out.println("Chunk "+c.getX()+","+c.getY()+","+c.getZ());
				System.out.println("Found "+cubex+","+cubey+","+cubez);
				System.out.println("Val: "+cube);
				if(BlockLibrary.isSolid(cube))
				{
					return new RaycastResult(currentFace,c.getX(),c.getY(),c.getZ(),cubex,cubey,cubez);
				}
			}
			currentDist=Math.sqrt((cx-ix)*(cx-ix) + (cy-iy)*(cy-iy) +(cz-iz)*(cz-iz));
		}
		return null;
	}
	public static class RaycastResult
	{
		private int px,py,pz;
		private int cx,cy,cz;
		private Block.facecode face;
		public RaycastResult(Block.facecode facecode,int cx,int cy,int cz,int px,int py,int pz)
		{
			this.face=facecode;
			this.px=px;
			this.py=py;
			this.pz=pz;
			this.cx=cx;
			this.cy=cy;
			this.cz=cz;
		}
		public int getX()
		{
			return this.px;
		}
		public int getY()
		{
			return this.py;
		}
		public int getZ()
		{
			return this.pz;
		}
		public int getChunkX()
		{
			return this.cx;
		}
		public int getChunkY()
		{
			return this.cy;
		}
		public int getChunkZ()
		{
			return this.cz;
		}
		public Block.facecode getFaceCode()
		{
			return this.face;
		}
	}
}