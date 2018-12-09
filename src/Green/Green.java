package Green;

import java.util.*;
import java.util.stream.Collectors;

import processing.core.*;
import processing.event.MouseEvent;

import static processing.core.PApplet.sin;
import static processing.core.PApplet.atan2;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.tan;

/**
 * The main class of the library. This handles inputs, provides a few utility functions, 
 * and provides a {@link World}-independent way to call {@link #handleAct()} and {@link #handleDraw()}.
 * <br><br>
 * A single instance of this class should be created in {@link processing.core.PApplet#setup()} by supplying '{@code this}' as the parameter.
 * @author Zacchary Dempsey-Plante
 */
public final class Green
{
	private static Green instance;
	private PApplet parent;
	
	private static World _currentWorld;
	
	private long _lastMillis = 0;
	private float _deltaTime = 0; //I don't like storing a dynamic variable like this, but it has to be stored so that calls to getDeltaTime() are consistent within frames
	
	//Input-related stuff
	private int _mouseX = 0;
	private int _mouseY = 0;
	private int _pmouseX = 0;
	private int _pmouseY = 0;
	private Set<Integer> _mouseButtonsDown = new HashSet<Integer>();
	private Set<Integer> _mouseButtonsDownInFrame = new HashSet<Integer>();
	private Set<Integer> _mouseButtonsUpInFrame = new HashSet<Integer>();
	private int _mouseScrollInFrame = 0;
	private Set<InputKey> _keysDown = new HashSet<InputKey>();
	private Set<InputKey> _keysDownInFrame = new HashSet<InputKey>();
	private Set<InputKey> _keysUpInFrame = new HashSet<InputKey>();
	
	//Image-resizing constants
	/**
	 * Bilinear resizing algorithm. This is great for upscaling pictures or other detailed images. This simply uses Processing's native {@link PImage#resize(int, int)}.
	 */
	public static final int BILINEAR = 0;
	/**
	 * Nearest-neighbour resizing algorithm. This is great for keeping pixel art looking crisp and sharp at higher resolutions. This uses the custom {@link Green#resizeNN(PImage, int, int)}.
	 */
	public static final int NEAREST_NEIGHBOR = 1;
	/**
	 * Tile the image to fit (from top left), rather than adjusting it in any way. This uses the custom {@link Green#tileImage(PImage, int, int)}.
	 */
	public static final int TILE = 2;
	
	//Constructors
	/**
	 * Starts the backbone of the library.
	 * @param theParent The {@link processing.core.PApplet} running the library.
	 * @throws SingleInstanceException Thrown when this constructor is called more than once.
	 */
	public Green(PApplet theParent)
	{
		if(instance != null) throw new SingleInstanceException();
		instance = this;
		parent = theParent;
	}
	
	//Getters
	/**
	 * Retrieves the current instance of the library that is running.
	 * @return The current instance of the library that is running.
	 */
	public static Green getInstance()
	{
		return instance;
	}
	/**
	 * Retrieves the currently-loaded {@link World}.
	 * @return The currently-loaded {@link World}.
	 */
	public static World getWorld()
	{
		return _currentWorld;
	}
	/**
	 * Retrieves the parent instance of Processing.
	 * @return The parent instance of Processing.
	 */
	public PApplet getParent()
	{
		return parent;
	}
	/**
	 * Retrieves the deltaTime - the time since the last frame.
	 * @return The time, in seconds, since the last frame.
	 */
	public float getDeltaTime()
	{
		return _deltaTime;
	}
	
	//Math Methods
	/**
	 * Calculates the distance between two points (({@code x1}, {@code y1}) and ({@code x2}, {@code y2})).
	 * @param x1 The X-axis position of point 1.
	 * @param y1 The Y-axis position of point 1.
	 * @param x2 The X-axis position of point 2.
	 * @param y2 The Y-axis position of point 2.
	 * @return The distance between the two points.
	 */
	public static float getPointsDist(float x1, float y1, float x2, float y2)
	{
		return (float) Math.hypot(x2 - x1, y2 - y1);
	}
	/**
	 * Calculates the angle between two points (({@code x1}, {@code y1}) and ({@code x2}, {@code y2})).
	 * @param x1 The X-axis position of point 1.
	 * @param y1 The Y-axis position of point 1.
	 * @param x2 The X-axis position of point 2.
	 * @param y2 The Y-axis position of point 2.
	 * @return The angle between the two points.
	 */
	public static float getPointsAngle(float x1, float y1, float x2, float y2)
	{
		if(x1 == x2 && y1 == y2) return 0f; //Because it is impossible to get the angle towards where you already are
		return atan2(y2 - y1, x2 - x1);
	}
	/**
	 * Determines whether two line segments ([({@code a1x}, {@code a1y}) - ({@code a2x}, {@code a2y})] and [({@code b1x}, {@code b1y}) - ({@code b2x}, {@code b2y})]) intersect.
	 * @param a1x The X-axis position of point 1 of line segment A.
	 * @param a1y The Y-axis position of point 1 of line segment A.
	 * @param a2x The X-axis position of point 2 of line segment A.
	 * @param a2y The Y-axis position of point 2 of line segment A.
	 * @param b1x The X-axis position of point 1 of line segment B.
	 * @param b1y The Y-axis position of point 1 of line segment B.
	 * @param b2x The X-axis position of point 2 of line segment B.
	 * @param b2y The Y-axis position of point 2 of line segment B.
	 * @return Whether or not the two line segments intersect.
	 */
	public static boolean getLinesIntersect(float a1x, float a1y, float a2x, float a2y, float b1x, float b1y, float b2x, float b2y)
	{
		float iX;
		float iY;
		
		float aM = (a2y - a1y) / (a2x - a1x);
		float aB = a1y - aM * a1x;
		float bM = (b2y - b1y) / (b2x - b1x);
		float bB = b1y - bM * b1x;
		
		if (a1x == a2x) iX = a1x;
		else if (b1x == b2x) iX = b1x;
		else
		{
			if (aM == bM) return false;
			
			iX = (bB - aB) / (aM - bM);
			//float iY = aM * iX + aB;
		}
		iY = !Float.isNaN(aM) && !Float.isInfinite(aM) ? aM * iX + aB : bM * iX + bB;
		
		return Math.min(a1x, a2x) <= iX && iX <= Math.max(a1x, a2x) && Math.min(b1x, b2x) <= iX && iX <= Math.max(b1x, b2x) &&
		       Math.min(a1y, a2y) <= iY && iY <= Math.max(a1y, a2y) && Math.min(b1y, b2y) <= iY && iY <= Math.max(b1y, b2y);
	}
	
	//Static Methods
	/**
	 * Calculates the cosecant of {@code angle} in radians.
	 * @param angle The angle in radians to calculate the cosecant of.
	 * @return The cosecant of {@code angle}.
	 */
	public static float csc(float angle)
	{
		return 1f / sin(angle);
	}
	/**
	 * Calculates the secant of {@code angle} in radians.
	 * @param angle The angle in radians to calculate the secant of.
	 * @return The secant of {@code angle}.
	 */
	public static float sec(float angle)
	{
		return 1f / cos(angle);
	}
	/**
	 * Calculates the cotangent of {@code angle} in radians.
	 * @param angle The angle in radians to calculate the cotangent of.
	 * @return The cotangent of {@code angle}.
	 */
	public static float cot(float angle)
	{
		return 1f / tan(angle);
	}
	
	//Utility Methods
	/**
	 * Calculates the number of digits in a given integer.
	 * @param value The integer to measure the number of digits of.
	 * @return The number of digits in {@code value}.
	 */
	public static int getDigits(int value)
	{
		return String.valueOf(value).length();
	}
	/**
	 * A nearest-neighbour resizing implementation. This is great for resizing pixel art while keeping it looking crisp and sharp.
	 * @param src The original image to resize.
	 * @param w The width to resize to.
	 * @param h The height to resize to.
	 * @return The resized image.
	 * @see <a href="https://gist.github.com/gncgnc/fc645b8db00ec43d43fecef37d58df73">https://gist.github.com/gncgnc/fc645b8db00ec43d43fecef37d58df73</a>
	 * @see <a href="https://gist.github.com/GoToLoop/2e12acf577506fd53267e1d186624d7c">https://gist.github.com/GoToLoop/2e12acf577506fd53267e1d186624d7c</a>
	 */
	public PImage resizeNN(PImage src, int w, int h)
	{
		//Sanitize dimension parameters
		w = (int) Math.floor(Math.abs(w));
		h = (int) Math.floor(Math.abs(h));
		
		//Quit prematurely if both dimensions are equal or parameters are both 0
		if (w == src.width && h == src.height || (w <= 0 && h <= 0)) return src;
		
		//Scale dimension parameters
		if (w <= 0) w = h * src.width / src.height | 0; //when only parameter w is 0
		if (h <= 0) h = w * src.height / src.width | 0; //when only parameter h is 0
		
		PImage img = parent.createImage(w, h, src.format); //Create a temporary image with the same pixel format as the source
		float sx = (float) w / src.width, sy = (float) h / src.height; //Scaled coords for current image
		
		src.loadPixels();
		img.loadPixels();
		
		int[] pixInt = src.pixels;
		int[] imgInt = img.pixels;
		
		for (int y = 0; y < h;)
		{
			int curRow = (int) (src.width * Math.floor(y / sy));
			int tgtRow = w * y++;

			for (int x = 0; x < w;)
			{
				int curIdx = (int) (curRow + Math.floor(x / sx));
				int tgtIdx = tgtRow + x++;
				imgInt[tgtIdx] = pixInt[curIdx];
			}
		}
		
		img.updatePixels(); //Update the return image with it's new pixels[] array
		return img;
	}
	/**
	 * Tiles a source image to fill a new image with different dimensions. Primarily used for background images.
	 * @param src The original image to tile.
	 * @param w The width to tile to.
	 * @param h The height to tile to.
	 * @return The new image, composed of {@code src} tiled to fit.
	 */
	public PImage tileImage(PImage src, int w, int h)
	{
		//Sanitize dimension parameters
		w = (int) Math.floor(Math.abs(w));
		h = (int) Math.floor(Math.abs(h));
		
		//Quit prematurely if both dimensions are equal or parameters are both 0
		if (w == src.width && h == src.height || (w <= 0 && h <= 0)) return src;
		
		PImage img = parent.createImage(w, h, src.format); //Create a temporary image with the same pixel format as the source
		
		int tW = (int) Math.ceil(w / src.width) + 1, tH = (int) Math.ceil(h / src.height) + 1;
		for (int y = 0; y < tH; y++)
			for (int x = 0; x < tW; x++)
				img.set(x * src.width, y * src.height, src);
		
		return img;
	}
	
	//Base Methods
	/**
	 * Loads a {@link World} to make it the one currently in use.
	 * @param world The {@link World} to load.
	 */
	public void loadWorld(World world)
	{
		_currentWorld = world;
		//parent.setSize(world.getWidth(), world.getHeight());
		world.prepare();
	}
	/**
	 * Calls the {@link Actor#draw()} method on every {@link Actor}, drawing all objects in the currently-loaded {@link World} to screen.
	 * @throws NoWorldException Thrown when the method is called and there is no {@link World} loaded.
	 */
	public void handleDraw() throws NoWorldException
	{
		World world = getWorld();
		if(world == null) throw new NoWorldException();
		world.handleDraw();
	}
	/**
	 * Calls the {@link Actor#act(float)} method on every {@link Actor} in the currently-loaded {@link World}.
	 * @throws NoWorldException Thrown when the method is called and there is no {@link World} loaded.
	 */
	public void handleAct() throws NoWorldException
	{
		long currentMillis = parent.millis();
		_deltaTime = (currentMillis - _lastMillis) / 1000f;
		_lastMillis = currentMillis;
		World world = getWorld();
		if(world == null) throw new NoWorldException();
		world.handleAct();
	}
	//Input
	/**
	 * Handles all of the frame-by-frame input.
	 */
	public final void handleInput()
	{
		_mouseButtonsDownInFrame.clear();
		_mouseButtonsUpInFrame.clear();
		_mouseScrollInFrame = 0;
		_keysDownInFrame.clear();
		_keysUpInFrame.clear();
	}
	//Input mouse
	/**
	 * Handles the event of a mouse button being pressed. Put this in the {@link PApplet#mousePressed()} event.
	 * @param mouseButton The mouse button that was pressed - either {@link PConstants#LEFT}, {@link PConstants#CENTER}, or {@link PConstants#RIGHT}. Please pass in {@link PApplet#mouseButton} in your code.
	 */
	public final void handleMouseDown(int mouseButton)
	{
		if(!_mouseButtonsDown.contains(mouseButton))
			_mouseButtonsDownInFrame.add(mouseButton);
		_mouseButtonsDown.add(mouseButton);
	}
	/**
	 * Handles the event of a mouse button being released. Put this in the {@link PApplet#mouseReleased()} event.
	 * @param mouseButton The mouse button that was released - either {@link PConstants#LEFT}, {@link PConstants#CENTER}, or {@link PConstants#RIGHT}. Please pass in {@link PApplet#mouseButton} in your code.
	 */
	public final void handleMouseUp(int mouseButton)
	{
		if(_mouseButtonsDown.contains(mouseButton))
			_mouseButtonsUpInFrame.add(mouseButton);
		_mouseButtonsDown.remove(mouseButton);
	}
	/**
	 * Handles the mouse position input. Just put this in your {@link PApplet#draw()} method.
	 * @param pmouseX The mouse's previous X-axis position, in pixels. Please pass in {@link PApplet#pmouseX} in your code.
	 * @param pmouseY The mouse's previous Y-axis position, in pixels. Please pass in {@link PApplet#pmouseY} in your code.
	 * @param mouseX The mouse's X-axis position, in pixels. Please pass in {@link PApplet#mouseX} in your code.
	 * @param mouseY The mouse's Y-axis position, in pixels. Please pass in {@link PApplet#mouseY} in your code.
	 */
	public final void handleMousePosition(int pmouseX, int pmouseY, int mouseX, int mouseY)
	{
		_pmouseX = pmouseX;
		_pmouseY = pmouseY;
		_mouseX = mouseX;
		_mouseY = mouseY;
	}
	/**
	 * Handles the mouse position input. Just put this in your {@link PApplet#draw()} method.
	 * @param mouseX The mouse's X-axis position, in pixels. Please pass in {@link PApplet#mouseX} in your code.
	 * @param mouseY The mouse's Y-axis position, in pixels. Please pass in {@link PApplet#mouseY} in your code.
	 * @deprecated Please use {@link #handleMousePosition(int, int, int, int)} instead.
	 */
	public final void handleMousePosition(int mouseX, int mouseY)
	{
		_pmouseX = _mouseX;
		_pmouseY = _mouseY;
		_mouseX = mouseX;
		_mouseY = mouseY;
	}
	/**
	 * Handles when the mouse is scrolled. Put this in the {@link PApplet#mouseWheel(processing.event.MouseEvent)} event.
	 * @param mouseScroll The amount that the mouse has scrolled this frame. Please pass in {@link MouseEvent#getCount()}.
	 */
	public final void handleMouseWheel(int mouseScroll)
	{
		_mouseScrollInFrame = mouseScroll;
	}
	/**
	 * Retrieves whether or not the mouse has moved this frame.
	 * @return Whether or not the mouse has moved this frame.
	 */
	public final boolean isMouseMoving()
	{
		return _pmouseX != _mouseX || _pmouseY != _mouseY;
	}
	/**
	 * Calculates the mouse's speed from last frame to this frame. This may not be very reliable, since it is calculated from frame-to-frame.
	 * @return The mouse's speed from last frame to this frame, in pixels/second.
	 */
	public final float getMouseSpeed()
	{
		return ((float) Math.hypot(_mouseX - _pmouseX, _mouseY - _pmouseY)) / _deltaTime;
	}
	/**
	 * Retrieves whether or not a specific mouse button is currently down.
	 * @param mouseButton The mouse button to check for - either {@link PConstants#LEFT}, {@link PConstants#CENTER}, or {@link PConstants#RIGHT}.
	 * @return Whether or not {@code mouseButton} is down.
	 */
	public final boolean isMouseButtonDown(int mouseButton)
	{
		return _mouseButtonsDown.contains(mouseButton);
	}
	/**
	 * Retrieves whether or not a specific mouse button was pressed this frame.
	 * @param mouseButton The mouse button to check for - either {@link PConstants#LEFT}, {@link PConstants#CENTER}, or {@link PConstants#RIGHT}.
	 * @return Whether or not {@code mouseButton} was pressed in this frame.
	 */
	public final boolean isMouseButtonDownThisFrame(int mouseButton)
	{
		return _mouseButtonsDownInFrame.contains(mouseButton);
	}
	/**
	 * Retrieves whether or not a specific mouse button was released this frame.
	 * @param mouseButton The mouse button to check for - either {@link PConstants#LEFT}, {@link PConstants#CENTER}, or {@link PConstants#RIGHT}.
	 * @return Whether or not {@code mouseButton} was released in this frame.
	 */
	public final boolean isMouseButtonUpThisFrame(int mouseButton)
	{
		return _mouseButtonsUpInFrame.contains(mouseButton);
	}
	/**
	 * Retrieves the amount that the mouse scrolled in this frame.
	 * @return The amount that the mouse scrolled in this frame.
	 */
	public final int getMouseScroll()
	{
		return _mouseScrollInFrame;
	}
	/**
	 * Retrieves whether or not the mouse is currently scrolling.
	 * @return Whether or not the mouse is currently scrolling.
	 */
	public final boolean isMouseScrolling()
	{
		return _mouseScrollInFrame != 0;
	}
	
	//Input keys
	/**
	 * Handles the event of a key being pressed. Put this in the {@link PApplet#keyPressed()} event.
	 * @param key The {@code char} representation of the key that was pressed. Please pass in {@link PApplet#key} in your code.
	 * @param keyCode They key code of the key that was pressed. Please pass in {@link PApplet#keyCode} in your code.
	 * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html">Java KeyEvent reference</a>
	 */
	public final void handleKeyDown(char key, int keyCode)
	{
		InputKey newKey = new InputKey(key, keyCode);
		if(!_keysDown.contains(newKey))
			_keysDownInFrame.add(newKey);
		_keysDown.add(newKey);
	}
	/**
	 * Handles the event of a key being released. Put this in the {@link PApplet#keyReleased()} event.
	 * @param key The {@code char} representation of the key that was released. Please pass in {@link PApplet#key} in your code.
	 * @param keyCode They key code of the key that was released. Please pass in {@link PApplet#keyCode} in your code.
	 * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html">Java KeyEvent reference</a>
	 */
	public final void handleKeyUp(char key, int keyCode)
	{
		InputKey newKey = new InputKey(key, keyCode);
		if(_keysDown.contains(newKey))
			_keysUpInFrame.add(newKey);
		_keysDown.remove(newKey);
	}
	/**
	 * Retrieves whether a specific key is currently down.
	 * @param key The {@code char} representation of the key to check for.
	 * @return Whether or not {@code key} is currently down.
	 */
	public final boolean isKeyDown(char key)
	{
		List<InputKey> result = _keysDown.stream()
				.filter(item -> item.getKey() == key)
				.collect(Collectors.toList());
		return result.size() > 0;
	}
	/**
	 * Retrieves whether a specific key is currently down.
	 * @param keyCode The key code of the key to check for.
	 * @return Whether or not {@code key} is currently down.
	 * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html">Java KeyEvent reference</a>
	 */
	public final boolean isKeyDown(int keyCode)
	{
		List<InputKey> result = _keysDown.stream()
				.filter(item -> item.getKeyCode() == keyCode)
				.collect(Collectors.toList());
		return result.size() > 0;
	}
	/**
	 * Retrieves whether a specific key is currently down.
	 * @param key The key to check for.
	 * @return Whether or not {@code key} is currently down.
	 */
	public final boolean isKeyDown(InputKey key)
	{
		return _keysDown.contains(key);
	}
	/**
	 * Retrieves whether or not a specific key was pressed this frame.
	 * @param key The {@code char} representation of the key to check for.
	 * @return Whether or not {@code key} was pressed in this frame.
	 */
	public final boolean isKeyDownThisFrame(char key)
	{
		List<InputKey> result = _keysDownInFrame.stream()
				.filter(item -> item.getKey() == key)
				.collect(Collectors.toList());
		return result.size() > 0;
	}
	/**
	 * Retrieves whether or not a specific key was pressed this frame.
	 * @param keyCode The key code of the key to check for.
	 * @return Whether or not {@code key} was pressed in this frame.
	 * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html">Java KeyEvent reference</a>
	 */
	public final boolean isKeyDownThisFrame(int keyCode)
	{
		List<InputKey> result = _keysDownInFrame.stream()
				.filter(item -> item.getKeyCode() == keyCode)
				.collect(Collectors.toList());
		return result.size() > 0;
	}
	/**
	 * Retrieves whether or not a specific key was pressed this frame.
	 * @param key The key to check for.
	 * @return Whether or not {@code key} was pressed in this frame.
	 */
	public final boolean isKeyDownThisFrame(InputKey key)
	{
		return _keysDownInFrame.contains(key);
	}
	/**
	 * Retrieves whether or not a specific key was released this frame.
	 * @param key The {@code char} representation of the key to check for.
	 * @return Whether or not {@code key} was released in this frame.
	 */
	public final boolean isKeyUpThisFrame(char key)
	{
		List<InputKey> result = _keysUpInFrame.stream()
				.filter(item -> item.getKey() == key)
				.collect(Collectors.toList());
		return result.size() > 0;
	}
	/**
	 * Retrieves whether or not a specific key was released this frame.
	 * @param keyCode The key code of the key to check for.
	 * @return Whether or not {@code key} was released in this frame.
	 * @see <a href="https://docs.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html">Java KeyEvent reference</a>
	 */
	public final boolean isKeyUpThisFrame(int keyCode)
	{
		List<InputKey> result = _keysUpInFrame.stream()
				.filter(item -> item.getKeyCode() == keyCode)
				.collect(Collectors.toList());
		return result.size() > 0;
	}
	/**
	 * Retrieves whether or not a specific key was released this frame.
	 * @param key The key to check for.
	 * @return Whether or not {@code key} was released in this frame.
	 */
	public final boolean isKeyUpThisFrame(InputKey key)
	{
		return _keysUpInFrame.contains(key);
	}
}

