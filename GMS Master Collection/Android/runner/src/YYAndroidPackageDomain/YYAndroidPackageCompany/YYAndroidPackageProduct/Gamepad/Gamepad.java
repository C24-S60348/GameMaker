package ${YYAndroidPackageName};

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.os.ParcelUuid;

import java.lang.IllegalAccessException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import com.yoyogames.runner.RunnerJNILib;
import ${YYAndroidPackageName}.IGamepadDevice;
import ${YYAndroidPackageName}.NYKODevice;
import ${YYAndroidPackageName}.iCade;

public class Gamepad
{	
	// Set to the minSdkVersion as specified in the AndroidManifest.xml (should we read this out somehow instead?)
	// If this isn't bumped up to at least 9 via prefs/BLUETOOTH permission then Gamepad support will be disabled
	static public int msMinAPILevel = 7;

	// Redefinition (not great, but API safe) of InputDevice constnaces
	private static final int SOURCE_CLASS_JOYSTICK = 0x00000010;	
	private static final int SOURCE_GAMEPAD = 0x00000401;
	private static final int SOURCE_DPAD = 0x00000201;	

	// List of devices that aren't iCade *specific*
	static ArrayList<IGamepadDevice> msGamepadList = new ArrayList<IGamepadDevice>();
	// List of devices that behave like an iCade. May contain entries from msGamepadList
	static ArrayList<BluetoothDevice> msiCadeList = new ArrayList<BluetoothDevice>();

	// InputDevice functionality only exists with API level >= 9
	static Method msGetDeviceMethod = null;

	// These will only be enumerated if getDevice() resolves for InputEvent
	static HashMap<String, Method> msInputDeviceMethods = null;
	static HashMap<String, Method> msMotionRangeMethods = null;
	static HashMap<String, Method> msMotionEventMethods = null;

	/*
	 * Checks that we're on API level >= 9 and sets up Method pointers if available
	 */
	private static void EnumerateAPILevel()
	{
		try {															
			Class<?> inputEventClass = Class.forName("android.view.InputEvent");

			// API level 9
			msGetDeviceMethod = inputEventClass.getDeclaredMethod("getDevice");
			if (msGetDeviceMethod != null) { 				
							
				msMinAPILevel = 9;
				
				Class<?> inputDeviceClass = Class.forName("android.view.InputDevice");
				msInputDeviceMethods = new HashMap<String, Method>();
				msInputDeviceMethods.put("getSources", inputDeviceClass.getDeclaredMethod("getSources"));
				msInputDeviceMethods.put("getName", inputDeviceClass.getDeclaredMethod("getName"));		

				Class<?> motionEventClass = Class.forName("android.view.MotionEvent");				
				Class parTypes[] = new Class[1];
        		parTypes[0] = Integer.TYPE;
				msMotionEventMethods = new HashMap<String, Method>();
				msMotionEventMethods.put("getAxisValue", motionEventClass.getDeclaredMethod("getAxisValue", parTypes));					

				Class<?> motionRangeClass = Class.forName("android.view.InputDevice$MotionRange");
				msMotionRangeMethods = new HashMap<String, Method>();
				msMotionRangeMethods.put("getAxis", motionRangeClass.getDeclaredMethod("getAxis"));
				msMotionRangeMethods.put("getRange", motionRangeClass.getDeclaredMethod("getRange"));
				msMotionRangeMethods.put("getMin", motionRangeClass.getDeclaredMethod("getMin"));
				msMotionRangeMethods.put("getMax", motionRangeClass.getDeclaredMethod("getMax"));
				

				Method getMotionRangesMethod = inputDeviceClass.getDeclaredMethod("getMotionRanges");
				if (getMotionRangesMethod != null) {

					msMinAPILevel = 12;
					msInputDeviceMethods.put("getMotionRanges", getMotionRangesMethod);
					msMotionRangeMethods.put("getSource", motionRangeClass.getDeclaredMethod("getSource"));
				}
			}
		}
		catch (Exception e) {
			Log.i("yoyo", "ERROR: Enumerating API level " + e.getMessage());
		}
	}

	/*
	 * Checks to see if the device name belongs to an iCade device
	 */
	private static boolean iCadeDeviceName(String deviceName)
	{
		if (deviceName.contains(" 8-bitty ") || 
			deviceName.contains(" iCade ") ||
			deviceName.contains(NYKODevice.DeviceDescriptor)) // spoofs iCades if the switch is set
		{
			return true;
		}
		return false;
	}

	/**
	 * Performed during the Application's onStart/onResume
	 */
	static public void CheckDeviceSupport(BluetoothDevice bd)
	{
		Log.i( "yoyo", "BluetoothDevice found - " + bd.getName() );
		if (bd.getName().contains(NYKODevice.DeviceDescriptor)) {
			
			NYKODevice device = new NYKODevice(bd);
			msGamepadList.add(device);

			// Device index should be +1 from that of this list to account for iCades being at slot 0
			Log.i("yoyo", "GAMEPAD: Registering device connected " + msGamepadList.size() + "," + device.ButtonCount() + "," + device.AxisCount());			
			RunnerJNILib.registerGamepadConnected(msGamepadList.size(), device.ButtonCount(), device.AxisCount());
		}
	}
	
	static public InputDevice findBySource(int sourceType) {
        int[] ids = InputDevice.getDeviceIds(); 

        // Return the first matching source we find...
		int i = 0;
        for (i = 0; i < ids.length; i++) {
			InputDevice dev = InputDevice.getDevice(ids[i]);
			int sources = dev.getSources();

			if ((sources & ~InputDevice.SOURCE_CLASS_MASK & sourceType) != 0) {
				return dev;
			}
        }
        
        return null;
	}
	
	static public InputDevice findJoystick() {
		return findBySource(InputDevice.SOURCE_JOYSTICK);
	}
	

	/**
	 * Performed during the Application's onStart/onResume
	 */
	static public void EnumerateDevices(IniBundle _prefs)
	{		
		if ((_prefs != null) && (_prefs.getBoolean("YYiCadeSupport"))) {			

			if (RunnerActivity.CurrentActivity.checkCallingOrSelfPermission("android.permission.BLUETOOTH") == 0) 
			{
				EnumerateAPILevel();

				Log.i("yoyo", "GAMEPAD: Bonded Bluetooth devices read");
				Set pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

				// Filter the paired devices down to devices we support
				msGamepadList.clear();

				Iterator iter = pairedDevices.iterator();
				while (iter.hasNext())
				{					
					BluetoothDevice bd = (BluetoothDevice)iter.next();
					String deviceName = bd.getName();

					Log.i("yoyo", "GAMEPAD: Found Bluetooth device " + deviceName);

					if (iCadeDeviceName(deviceName))
					{
						msiCadeList.add(bd);
					}
					// NB: It is entirely valid for certain devices to appear in both lists (see NYKO iCade spoofing)
					CheckDeviceSupport(bd);
				}
			}
			else {
				Log.i("yoyo", "ERROR: Bluetooth permission not available");
			}
		}
		else {
			Log.i("yoyo", "iCade Support in \"Global Game Settings/Android\" not selected");
		}

		// RK :: Code from nVidia to find joysticks (and gamepads) 
		boolean hasJoystickMethods = false;
		try {
			Method level12Method = KeyEvent.class.getMethod(
				"keyCodeToString", new Class[] { int.class } ); 
			hasJoystickMethods = (level12Method != null);
			Log.d("yoyo", "****** Found API level 12 function! Joysticks supported");
		} catch (NoSuchMethodException nsme) {
			Log.d("yoyo", "****** Did not find API level 12 function! Joysticks NOT supported!");
		}

		if (hasJoystickMethods)
		{
			InputDevice joystick = findJoystick();
			if (joystick != null) {
				Log.i("yoyo", "Joystick found - \"" + joystick.getName() + "\"");

				IGamepadDevice device = null;
				if (joystick.getName().contains("nvidia_Corporation nvidia_joypad")) {
					 device = new nVidiaShieldDevice(joystick.getName());
				}  // end if
				else {
					 device = new GenericDevice(joystick.getName());
				} // end else
				
				if (device != null) {
					msGamepadList.add(device);
					RunnerJNILib.registerGamepadConnected(msGamepadList.size(), device.ButtonCount(), device.AxisCount());		
				} // end if
			} // end if
		} // end if
			
		Log.i("yoyo", "GAMEPAD: Enumeration complete");
	}

	/*
	 * Works out how many devices GM should display as available
	 */
	static public int DeviceCount()
	{
		// Always count an iCade whether or not we've found one attached
		return msGamepadList.size() + 1;
	}

	/*
	 * Return the non-iCade device index at the GM level
	 */
	static private IGamepadDevice GetGamepadDevice(String deviceName)
	{
		for (int n = 0; n < msGamepadList.size(); n++) {		

			IGamepadDevice device = msGamepadList.get(n);
			if (device.getName().equals(deviceName)) {
				return device;
			}
		}
		
		Log.i( "yoyo", "GAMEPAD DEVICE not found! - " + deviceName + " registering it as a generic device");
		GenericDevice device = new GenericDevice(deviceName);
		msGamepadList.add(device);
		RunnerJNILib.registerGamepadConnected(msGamepadList.size(), device.ButtonCount(), device.AxisCount());		
		return device;
	}

	/** 
	 * Return the name of a valid gamepad device if one is paired
	 */
	public static String GetDescriptor(int deviceIndex)
	{
		if (deviceIndex == 0) {

			if (msiCadeList.size() > 0) {
				return msiCadeList.get(0).getName();
			}
		}
		else {
			return msGamepadList.get(deviceIndex - 1).getName();
		}
		return "";
	}

	/**
	 * Respond to dispatchKeyEvent() from RunnerActivity
	 */
	static public void handleKeyEvent(KeyEvent ev)
	{		
		// Log.i("yoyo", "KEYEVENT: " + ev.getKeyCode() + " " + ev.keyCodeToString(ev.getKeyCode()));

		// Let iCade inputs override general gamepad handling
		if (!iCade.translateKeyEvent(ev)) 
		{
			if (msMinAPILevel >= 9) 
			{				
				try 
				{
					Object inputDevice = msGetDeviceMethod.invoke(ev);					
					Object deviceSources = msInputDeviceMethods.get("getSources").invoke(inputDevice);

					if ((((Integer)deviceSources & SOURCE_CLASS_JOYSTICK) != 0) ||
						(((Integer)deviceSources & SOURCE_GAMEPAD) != 0) ||
						(((Integer)deviceSources & SOURCE_DPAD) != 0))
					{
						// Work out the device index
						IGamepadDevice device = GetGamepadDevice((String)msInputDeviceMethods.get("getName").invoke(inputDevice));						
						if (device != null) {
							device.onButtonUpdate(ev.getKeyCode(), ev.getAction() == KeyEvent.ACTION_DOWN);
						}
					}
				}
				catch (Exception e) {
					Log.i("yoyo", "ERROR: " + e.getMessage());
				}
			}
		}		
	}

	/**
	 * Respond to dispatchGenericMotionEvent() from RunnerActivity
	 */
	static public void handleMotionEvent(MotionEvent ev)
	{		
		try 
		{			
			if (msMinAPILevel >= 12) 				
			{
				// Something has changed for the InputDevice but we can't tell from the MotionEvent what
				// therefore we're just going to read out all the axes and update GM as such
				Object inputDevice = msGetDeviceMethod.invoke(ev);
				String deviceName = (String)msInputDeviceMethods.get("getName").invoke(inputDevice);	

				// Work out the device index
				IGamepadDevice device = GetGamepadDevice((String)msInputDeviceMethods.get("getName").invoke(inputDevice));				
				if (device != null)
				{
					List<Object> motionRanges = (List<Object>)msInputDeviceMethods.get("getMotionRanges").invoke(inputDevice);
					for (Object motionRange : motionRanges) {

						int rangeSource = (Integer)(msMotionRangeMethods.get("getSource").invoke(motionRange));					
						if (((rangeSource & SOURCE_CLASS_JOYSTICK) != 0) ||
							((rangeSource & SOURCE_GAMEPAD) != 0))
						{
							int axisId = (Integer)msMotionRangeMethods.get("getAxis").invoke(motionRange);
							float axisValue = (Float)msMotionEventMethods.get("getAxisValue").invoke(ev, axisId);
							float axisRange = (Float)msMotionRangeMethods.get("getRange").invoke(motionRange);
							float axisMin = (Float)msMotionRangeMethods.get("getMin").invoke(motionRange);
							float axisMax = (Float)msMotionRangeMethods.get("getMax").invoke(motionRange);

							device.onAxisUpdate(axisId, axisValue, axisRange, axisMin, axisMax);
						}
					}
				}
			}			
		}
		catch (Exception e) {
			Log.i("yoyo", "ERROR: " + e.getMessage());
		}
	}

	/**
	 * Check for device availability
	 */
	static public boolean DeviceConnected(int deviceIndex)
	{
		if (deviceIndex == 0) {
			return (msiCadeList.size() != 0);
		}		
		return (msGamepadList.get(deviceIndex - 1) != null);
	}

	/**
	 * Check for device availability
	 */
	public static float[] GetButtonValues(int deviceIndex)
	{		
		if (deviceIndex == 0) {
			// WTF, WTF? Don't ask an iCade this :(
			throw new IllegalArgumentException("iCade index not valid for GetButtonValues");
		}
		return msGamepadList.get(deviceIndex - 1).GetButtonValues();
	}

	/**
	 * Check for device availability
	 */
	public static float[] GetAxesValues(int deviceIndex)
	{
		if (deviceIndex == 0) {
			// WTF, WTF? Don't ask an iCade this :(
			throw new IllegalArgumentException("iCade index not valid for GetButtonValues");
		}
		return msGamepadList.get(deviceIndex - 1).GetAxesValues();
	}

	public static int GetGamepadGMLMapping(int deviceIndex, int inputId)
	{
		if (deviceIndex == 0) {
			// iCade... no!??
			throw new IllegalArgumentException("iCade index not valid for GetGamepadGMLMapping");
		}
		return msGamepadList.get(deviceIndex - 1).GetGMLMapping(inputId);
	}
}