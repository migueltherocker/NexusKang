/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.teamdouche.nexuskang.nexus;

import static android.renderscript.Element.RGBA_8888;
import static android.renderscript.ProgramStore.DepthFunc.ALWAYS;
import static android.renderscript.Sampler.Value.LINEAR;
import static android.renderscript.Sampler.Value.CLAMP;
import static android.renderscript.Sampler.Value.WRAP;

import com.teamdouche.nexuskang.R;
import com.teamdouche.nexuskang.RenderScriptScene;

import android.content.res.Resources;
import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IHardwareService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.renderscript.Allocation;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramStore;
import android.renderscript.ProgramVertex;
import android.renderscript.Sampler;
import android.renderscript.Script;
import android.renderscript.ScriptC;
import android.renderscript.Type;
import android.renderscript.ProgramStore.BlendDstFunc;
import android.renderscript.ProgramStore.BlendSrcFunc;

import java.util.TimeZone;

class NexusRS extends RenderScriptScene implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int RSID_STATE = 0;

    private static final int RSID_COMMAND = 1;

    private static final int TEXTURES_COUNT = 2; // changed number of textures
                                                 // from 6 to 7

    private final BitmapFactory.Options mOptionsARGB = new BitmapFactory.Options();

    private static final String DEFAULT_BACKGROUND = "droid"; // blue droid

    private static Context mContext;

    private SharedPreferences mPrefs;

    private ProgramFragment mPfTexture;

    private ProgramFragment mPfTexture565;

    private ProgramFragment mPfColor;

    private ProgramStore mPsSolid;

    private ProgramStore mPsBlend;

    private ProgramVertex mPvOrtho;

    private ProgramVertex.MatrixAllocation mPvOrthoAlloc;

    private Sampler mClampSampler;

    private Sampler mWrapSampler;

    private Allocation mState;

    private Type mStateType;

    private WorldState mWorldState;

    private Allocation mCommandAllocation;

    private Type mCommandType;

    private CommandState mCommand;

    private Allocation[] mTextures = new Allocation[TEXTURES_COUNT];

    public Preset mPreset;

    public NexusRS(Context context, int width, int height) {
        super(width, height);

        mContext = context;
        mPrefs = mContext.getSharedPreferences(NexusWallpaper.SHARED_PREFS_NAME, 0);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mPreset = buildColors();

        mOptionsARGB.inScaled = false;
        mOptionsARGB.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    static class Preset {
        /**
         * @param String[] values: String array of HEX color values (ex:
         *            "#FFFFFF").
         */
        public Preset(String[] values) {
            super();

            int color0 = Integer.decode(values[0]).intValue();
            this.color0r = ((color0 >> 16) & 0xFF) / 255.0f;
            this.color0g = ((color0 >> 8) & 0xFF) / 255.0f;
            this.color0b = (color0 & 0xFF) / 255.0f;

            int color1 = Integer.decode(values[1]).intValue();
            this.color1r = ((color1 >> 16) & 0xFF) / 255.0f;
            this.color1g = ((color1 >> 8) & 0xFF) / 255.0f;
            this.color1b = (color1 & 0xFF) / 255.0f;

            int color2 = Integer.decode(values[2]).intValue();
            this.color2r = ((color2 >> 16) & 0xFF) / 255.0f;
            this.color2g = ((color2 >> 8) & 0xFF) / 255.0f;
            this.color2b = (color2 & 0xFF) / 255.0f;

            int color3 = Integer.decode(values[3]).intValue();
            this.color3r = ((color3 >> 16) & 0xFF) / 255.0f;
            this.color3g = ((color3 >> 8) & 0xFF) / 255.0f;
            this.color3b = (color3 & 0xFF) / 255.0f;
        }

        public float color0r, color0g, color0b;

        public float color1r, color1g, color1b;

        public float color2r, color2g, color2b;

        public float color3r, color3g, color3b;
    }

    /*
     * Build an array of Presets dynamically from XML.
     * @author Chris Soyars / Steve Kondik
     * @return Array of Preset instances.
     */
    private Preset buildColors() {
        final Resources res = mContext.getResources();
	String colors[] = new String[] {
	        mPrefs.getString("color0","#66CCDD"),
                mPrefs.getString("color1","#3366AA"),
                mPrefs.getString("color2","#6699AA"),
                mPrefs.getString("color3","#AABBBB")
	};
        final Preset preset = new Preset(colors);
        return preset;
    }

    private void setBackground(String resourceName) {

        // For compatibility with previous versions
        if (resourceName == null || "normal".equals(resourceName)) {
            resourceName = DEFAULT_BACKGROUND;
        }

        final Resources res = mContext.getResources();
        int bgId = res.getIdentifier(resourceName + "_background", "drawable",
                "com.teamdouche.nexuskang");
        final Allocation bg = loadTextureARGB(bgId, "TBackground");
        bg.uploadToTexture(0);
    }

    @Override
    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        mWorldState.xOffset = xOffset;
        mState.data(mWorldState);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height); // updates mWidth, mHeight

        // android.util.Log.d("NexusRS", String.format("resize(%d, %d)", width, height));

        mWorldState.width = width;
        mWorldState.height = height;
        mWorldState.rotate = width > height ? 1 : 0;
        mState.data(mWorldState);

        mPvOrthoAlloc.setupOrthoWindow(mWidth, mHeight);
    }

    @Override
    protected ScriptC createScript() {
        createProgramVertex();
        createProgramFragmentStore();
        createProgramFragment();
        createState();
        loadTextures();

        ScriptC.Builder sb = new ScriptC.Builder(mRS);
        sb.setType(mStateType, "State", RSID_STATE);
        sb.setType(mCommandType, "Command", RSID_COMMAND);

        sb.setScript(mResources, R.raw.nexus);

        Script.Invokable invokable = sb.addInvokable("initPulses");
        sb.setRoot(true);

        ScriptC script = sb.create();
        script.setClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        script.setTimeZone(TimeZone.getDefault().getID());

        script.bindAllocation(mState, RSID_STATE);
        script.bindAllocation(mCommandAllocation, RSID_COMMAND);

        invokable.execute();

        return script;
    }

    static class WorldState {
        public int width;

        public int height;

        public float glWidth;

        public float glHeight;

        public int rotate;

        public int isPreview;

        public float xOffset;

        public float color0r, color0g, color0b;

        public float color1r, color1g, color1b;

        public float color2r, color2g, color2b;

        public float color3r, color3g, color3b;

        public int mode;
        
        public float speed;
 
    }

    static class CommandState {
        public int x;

        public int y;

        public int command;

        public int pSize, hGlowSize, tailSize;
        
        public float sVariance;
    }

    private void makeNewState() {
        mWorldState.width = mWidth;
        mWorldState.height = mHeight;
        mWorldState.rotate = mWidth > mHeight ? 1 : 0;
        mWorldState.isPreview = isPreview() ? 1 : 0;

        

        mWorldState.color0r = mPreset.color0r;
        mWorldState.color0g = mPreset.color0g;
        mWorldState.color0b = mPreset.color0b;
        mWorldState.color1r = mPreset.color1r;
        mWorldState.color1g = mPreset.color1g;
        mWorldState.color1b = mPreset.color1b;
        mWorldState.color2r = mPreset.color2r;
        mWorldState.color2g = mPreset.color2g;
        mWorldState.color2b = mPreset.color2b;
        mWorldState.color3r = mPreset.color3r;
        mWorldState.color3g = mPreset.color3g;
        mWorldState.color3b = mPreset.color3b;

    // custom states
        mWorldState.speed = .01f * mPrefs.getInt("laserSpeed", 20);
        mCommand.pSize = 14 * mPrefs.getInt("laserSize", 1);
        mCommand.hGlowSize = mPrefs.getInt("glowSize", 32);
        mCommand.tailSize = mPrefs.getInt("tailSize", 40);
        mCommand.sVariance = 0.1f * mPrefs.getInt("speedVariance", 30);
      //  mCommand.mSweep = mPrefs.getInt("pref_sweepers",0);

      //  mWorldState.pSize = 30;

       // mWorldState.rectglow = 1;
    }

    private void createState() {
        mCommand = new CommandState();
        mWorldState = new WorldState();
        mWorldState.mode = 0; // standard nexus mode
        makeNewState();

        mStateType = Type.createFromClass(mRS, WorldState.class, 1, "WorldState");
        mState = Allocation.createTyped(mRS, mStateType);
        mState.data(mWorldState);

        mCommand.x = -1;
        mCommand.y = -1;

        mCommand.command = 0;
        
        mCommandType = Type.createFromClass(mRS, CommandState.class, 1, "DropState");
        mCommandAllocation = Allocation.createTyped(mRS, mCommandType);
        mCommandAllocation.data(mCommand);

    }

    private void loadTextures() {

        mTextures[0] = loadTextureARGB(R.drawable.pulse, "TPulse");
        mTextures[1] = loadTextureARGB(R.drawable.glow, "TGlow");

        final int count = mTextures.length;
        for (int i = 0; i < count; i++) {
            mTextures[i].uploadToTexture(0);
        }

        setBackground(mPrefs.getString("nexus_background", DEFAULT_BACKGROUND));

    }

    private Allocation loadTextureARGB(int id, String name) {
        Bitmap b = BitmapFactory.decodeResource(mResources, id, mOptionsARGB);
        final Allocation allocation = Allocation.createFromBitmap(mRS, b, RGBA_8888(mRS), false);
        allocation.setName(name);
        return allocation;
    }

    private void createProgramFragment() {
        // sampler and program fragment for pulses
        Sampler.Builder sampleBuilder = new Sampler.Builder(mRS);
        sampleBuilder.setMin(LINEAR);
        sampleBuilder.setMag(LINEAR);
        sampleBuilder.setWrapS(WRAP);
        sampleBuilder.setWrapT(WRAP);
        mWrapSampler = sampleBuilder.create();
        ProgramFragment.Builder builder = new ProgramFragment.Builder(mRS);
        builder.setTexture(ProgramFragment.Builder.EnvMode.MODULATE,
                ProgramFragment.Builder.Format.RGBA, 0);
        mPfTexture = builder.create();
        mPfTexture.setName("PFTexture");
        mPfTexture.bindSampler(mWrapSampler, 0);

        builder = new ProgramFragment.Builder(mRS);
        builder.setTexture(ProgramFragment.Builder.EnvMode.REPLACE,
                ProgramFragment.Builder.Format.RGB, 0);
        mPfColor = builder.create();
        mPfColor.setName("PFColor");
        mPfColor.bindSampler(mWrapSampler, 0);

        // sampler and program fragment for background image
        sampleBuilder.setWrapS(CLAMP);
        sampleBuilder.setWrapT(CLAMP);
        mClampSampler = sampleBuilder.create();
        builder = new ProgramFragment.Builder(mRS);
        builder.setTexture(ProgramFragment.Builder.EnvMode.MODULATE,
                ProgramFragment.Builder.Format.RGB, 0);
        mPfTexture565 = builder.create();
        mPfTexture565.setName("PFTexture565");
        mPfTexture565.bindSampler(mClampSampler, 0);
    }

    private void createProgramFragmentStore() {
        ProgramStore.Builder builder = new ProgramStore.Builder(mRS, null, null);
        builder.setDepthFunc(ALWAYS);
        builder.setBlendFunc(BlendSrcFunc.ONE, BlendDstFunc.ONE);
        builder.setDitherEnable(false);
        builder.setDepthMask(true);
        mPsSolid = builder.create();
        mPsSolid.setName("PSSolid");

        builder = new ProgramStore.Builder(mRS, null, null);
        builder.setDepthFunc(ALWAYS);
        // builder.setBlendFunc(BlendSrcFunc.SRC_ALPHA,
        // BlendDstFunc.ONE_MINUS_SRC_ALPHA);
        builder.setBlendFunc(BlendSrcFunc.SRC_ALPHA, BlendDstFunc.ONE);

        builder.setDitherEnable(false);
        builder.setDepthMask(true);
        mPsBlend = builder.create();
        mPsBlend.setName("PSBlend");
    }

    private void createProgramVertex() {
        mPvOrthoAlloc = new ProgramVertex.MatrixAllocation(mRS);
        mPvOrthoAlloc.setupOrthoWindow(mWidth, mHeight);

        ProgramVertex.Builder pvb = new ProgramVertex.Builder(mRS, null, null);
        pvb.setTextureMatrixEnable(true);
        mPvOrtho = pvb.create();
        mPvOrtho.bindAllocation(mPvOrthoAlloc);
        mPvOrtho.setName("PVOrtho");
    }

    @Override
    public Bundle onCommand(String action, int x, int y, int z, Bundle extras,
            boolean resultRequested) {

        final int dw = mWorldState.width;
        final int bw = 960;
        x = (int) (x + mWorldState.xOffset * (bw - dw));

        if ("android.wallpaper.tap".equals(action)) {
            IHardwareService hardware = IHardwareService.Stub.asInterface(ServiceManager.getService("hardware"));
            
            // Get the colors from the preset
            int colorR = (int) (mPreset.color0r * 255.0);                                                                        
            int colorG = (int) (mPreset.color0g * 255.0);                                                            
            int colorB = (int) (mPreset.color0b * 255.0);
                                   
            int colorValue = Color.rgb(colorR, colorG, colorB); 
            /*            
            try {
                // flash the trackball on tap
                hardware.pulseBreathingLightColor(colorValue); 
                
            } catch (RemoteException re) {
                Log.e("NexusLWP", "Could not preview LED color", re);
            }
	    */
            sendCommand(1, x, y);
        } else if ("android.home.drop".equals(action)) {
            sendCommand(2, x, y);
        }
        return null;
    }

    private void sendCommand(int command, int x, int y) {
        mCommand.x = x;
        mCommand.y = y;
        mCommand.command = command;
        mCommandAllocation.data(mCommand);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        try {
            if (!(key.equals("nexus_background")||key.equals("laserSize")||key.equals("glowSize")
              ||key.equals("tailSize")||key.equals("speedVariance"))) {
                mPreset = buildColors();
                makeNewState();
                mState.data(mWorldState);
            } else {
                setDirty(true);
            }
        } catch (Exception e){
            Log.e("RS","CRAAASSHHH", e);
        }
    }
}
