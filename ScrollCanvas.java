import java.io.*;

import javax.microedition.io.*;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.Sprite;

public class ScrollCanvas extends Canvas {
    public static final int ROTATE_CCW = 2;
    public static final int ROTATE_CW  = 1;
    
    public static final int ORIENT_UP = 0;
    public static final int ORIENT_RIGHT = 1;
    public static final int ORIENT_DOWN = 2;
    public static final int ORIENT_LEFT = 3;
    
    // scroll amount
    int                 xscroll;
    int                 yscroll;
    private boolean     showZoomStatus = false;
    private boolean     isFullScreen   = true;
    private int         dispWidth, dispHeight;
    FileConnection      fconn;
    Image               im;
    Image               imoriginal;
    private final ImageViewer midlet;

    // private Command salir;
    // private Command zoom;
    private int x, y, w, h;
    private int zoomlevel = 1;
    int         orientation;
    
    public ScrollCanvas(ImageViewer mid, String filename) {
        setFullScreenMode(isFullScreen);
        dispWidth = getWidth();
        dispHeight = getHeight();

        // scroll 1/3 screen size
        xscroll = dispWidth / 3;
        yscroll = dispHeight / 3;

        this.midlet = mid;

        final String filen = filename;

        try {
            System.out.println("Connector.open(" + filen);
            fconn = (FileConnection) Connector.open(filen, Connector.READ);

            InputStream in = fconn.openInputStream();

            imoriginal = Image.createImage(in);
            in.close();
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

        x = 0;
        y = 0;
        im = Image.createImage(imoriginal);
        w = im.getWidth();
        h = im.getHeight();
        orientation = ORIENT_UP;
    }

    public void paint(Graphics g) {
        if(im == null) {
            return;
        }
        
        String zm = "";

        g.setColor(255, 255, 255);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(0, 0, 0);
        g.drawImage(im, x, y, Graphics.TOP | Graphics.LEFT);

        switch (zoomlevel) {
        case 1 :
            zm = "100%";
            break;

        case 2 :
            zm = "50%";
            break;

        case 3 :
            zm = "33%";
            break;

        case 4 :
            zm = "25%";
            break;

        case 5 :
            zm = "fit to width";
            break;
        }

        if (showZoomStatus) {
            g.drawString("zoom: " + zm, 0, 0, Graphics.TOP | Graphics.LEFT);
            showZoomStatus = false;
        }
    }

    protected void keyPressed(int keyCode) {
        scrollImage(keyCode, 1.3);
    }

    protected void keyRepeated(int keyCode) {
        scrollImage(keyCode, 1);
    }

    void scrollImage(int keyCode, double scrollAccel) {
        int keyAction;
        try {
            keyAction = getGameAction(keyCode);           
        } catch (IllegalArgumentException e) {
            System.out.println("keyCode:" + keyCode + " caused IllegalArgumentException in getGameAction(keyCode)");                        
            keyAction = 0;
        }

        if (keyAction == 0) {
            keyAction = keyCode;
        }

//      Alert a = new Alert("keyAction = " + keyAction);
//      Display.getDisplay(midlet).setCurrent(a, this);
        switch (keyAction) {

        // (x,y) im anchor relative to canvas
        case Canvas.UP :
        case Canvas.KEY_NUM2 :
            y += yscroll * scrollAccel;

            break;

        case Canvas.RIGHT :
        case Canvas.KEY_NUM6 :
            x -= xscroll * scrollAccel;

            break;

        case Canvas.DOWN :
        case Canvas.KEY_NUM8 :
            y -= yscroll * scrollAccel;

            break;

        case Canvas.LEFT :
        case Canvas.KEY_NUM4 :
            x += xscroll * scrollAccel;

            break;

        case Canvas.FIRE :
        case Canvas.KEY_NUM5 :
            isFullScreen = !isFullScreen;
            setFullScreenMode(isFullScreen);

            break;

        case Canvas.KEY_STAR :
        case Canvas.GAME_C :
            midlet.commandAction(midlet.zoomout, this);

            return;

        case Canvas.KEY_POUND :
        case Canvas.GAME_D :
            midlet.commandAction(midlet.zoomin, this);

            return;

        case Canvas.KEY_NUM0 :
            midlet.commandAction(midlet.rotate, this);

            return;

        default :
            System.out.println("keyAction:" + keyAction);
        }

        System.out.println("im height:" + im.getHeight() + " dispHeight:" + dispHeight);
        y = Math.max(y, -im.getHeight() + dispHeight);
        x = Math.max(x, -im.getWidth() + dispWidth);

        if (y > 0) {
            y = 0;
        }

        if (x > 0) {
            x = 0;
        }

        System.out.println("xy = " + x + ":" + y);
        repaint();
    }

    public void zoomin() {
        if (zoomlevel != 1) {
            zoomlevel--;
        }
        rescaleImage(zoomlevel);
    }
 
    public void zoomout() {
        if (zoomlevel != 5) {
            zoomlevel++;
        }
        rescaleImage(zoomlevel);
    }
    
    public void rescaleImage(int zoom) {

        /*
         *  zoomlevel
         * 1=100%
         * 2=50%
         * 3=33%
         * 4=25%
         * 5=fit to screen
         */

        showZoomStatus = true;
        zoomlevel      = zoom;
        im             = imoriginal;
        w              = im.getWidth();
        h              = im.getHeight();
        x              = 0;
        y              = 0;
        orientation    = ORIENT_UP;

        if (zoom == 1) {           // return original image (fasterrrrrrrrrrrr)!!
            repaint();

            return;
        } else if (zoom == 5) {    // fit to screen
            zoom = w / getWidth();
        }

        im             = null;
        System.gc();
        
//      double d_zoom = (double) zoom;  
        int d_zoom = zoom;

//      if (zoom == 0) {
//          d_zoom = .5;    // double zoom, unused, causes out of memory
//      }
        int      width    = (int) (w / d_zoom);
        int      height   = (int) (h / d_zoom);
        Image    newImage;
        
        try { newImage = Image.createImage(width, height); }
        catch (OutOfMemoryError e) { 
            midlet.alert("Out of Memory! [resi]");
            System.gc();
            im = imoriginal;
            return; 
        }
        
        Graphics g        = newImage.getGraphics();

        System.out.println("zoomlevel: " + zoom + " width: " + width + " height: " + height);

        int rgb[]     = new int[w];
        int rgb_buf[] = new int[width];

        for (int y = 0; y < height; y++) {
            imoriginal.getRGB(rgb, 0, w, 0, y * d_zoom, w, 1);

            for (int x = 0; x < width; x++) {
                rgb_buf[x] = rgb[x * d_zoom];
            }

            g.drawRGB(rgb_buf, 0, width, 0, y, width, 1, false);

// too slow
//          for (int x = 0; x < width; x++) {
//
//              // void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height)
//              imoriginal.getRGB(rgb, 0, w, (int) (x * d_zoom), (int) (y * d_zoom), 1, 1);
//              g.drawRGB(rgb, 0, width, x, y, 1, 1, false);
//          }
        }

        System.out.println("zoom finish");
        im = newImage;
        w  = im.getWidth();
        h  = im.getHeight();
        repaint();
    }

    // low memory rotate at 100% zoom
    private void rotateImageInPlace(int direction) 
    {
        final int nextOrientation;
        final int currOrientation = orientation;
        
        System.out.println("riip: " + direction);
        
        if(direction == ROTATE_CW) {
            nextOrientation = (orientation+1)%4;
        } else if(direction == ROTATE_CCW) {
            nextOrientation = (orientation-1+4)%4;  //+4 because % returns negative if dividend < 0 
        } else { return; }
        
        im = null;
        System.gc();
      
        try {
            im = rotateImage(imoriginal, nextOrientation);
            rotateImageViewCenter(currOrientation, nextOrientation); // recalcs x,y
            w = im.getWidth();
            h = im.getHeight();

        } catch (OutOfMemoryError e) {
            // try again
            System.gc();

            try {
                im = rotateImage(imoriginal, nextOrientation);
                rotateImageViewCenter(currOrientation, nextOrientation);
                w = im.getWidth();
                h = im.getHeight();
            } catch (OutOfMemoryError e2) {
                midlet.alert("Out of Memory! [riip]");
                Runnable runn = new Runnable() {
                    public void run() {
                        System.gc();
                        im = imoriginal;
                        w = im.getWidth();
                        h = im.getHeight();
                        orientation = ScrollCanvas.ORIENT_UP;
                        repaint();
//                        midlet.commandAction(midlet.back, ScrollCanvas.this);
                    }
                };
                delayCallSerially(1000, runn);
                return;
            }
        }

        repaint();
        
    }
    
    void delayCallSerially(final long msec, final Runnable runn)
    {
        new Thread(
            new Runnable() {
                public void run() {
                    synchronized(Thread.currentThread()) {
                        try { Thread.currentThread().wait(msec); } 
                        catch (Exception ew) { System.out.println("dcs:" + ew); }
                    }
                    Display.getDisplay(midlet).callSerially(runn);
                } 
            }

        ).start();
    }

    // calculate the new (-x,-y) top left rotated image 
    private void rotateImageViewCenter(int orientation, int nextOrientation)
    {
        int dispHeight_2 = dispHeight/2;
        int dispWidth_2 = dispWidth/2;
        int cx, cy; // center
        int x_anchor = -this.x;
        int y_anchor = -this.y;
        int h = this.h;
        int w = this.w;
        
        cx = x_anchor + dispWidth_2;
        cy = y_anchor + dispHeight_2;
        
        // CW rotate the center point until we reach nextOrientation
        for(; orientation%4 != nextOrientation; orientation++) {
            // each rotation swaps h,w
            int temp = w; w = h; h = temp;
            int cy0 = cy; cy = cx; cx = w - cy0;            
        }
        
        x_anchor = cx - dispWidth_2;
        y_anchor = cy - dispHeight_2;
        
        this.x = -x_anchor;
        this.y = -y_anchor;
    }
    
    // h, w is dimension of imSrc
    private Image rotateImage (Image imSrc, int orientation) 
            throws OutOfMemoryError
    {

//      public void drawRegion(Image src,
//                             int x_src,
//                             int y_src,
//                             int width,
//                             int height,
//                             int transform,
//                             int x_dest,
//                             int y_dest,
//                             int anchor)
        System.out.println("ri2: " + orientation);
        // out of memory if rotate all at one shot, so draw in 100x100 regions
        int transform;
        Image newImage;
        Image im = imoriginal;
        int w_imorig  = im.getWidth();
        int h_imorig  = im.getHeight();

        this.orientation = orientation;
        
        if (orientation == ORIENT_RIGHT) {
            newImage = Image.createImage(h_imorig, w_imorig);
            Graphics g = newImage.getGraphics();
            transform = Sprite.TRANS_ROT90;

            for (int ychunk = 0; ychunk * 100 < h_imorig; ychunk++) {
                int destx = Math.max(0, h_imorig - ychunk * 100 - 100);
                int regionh = Math.min(100, h_imorig - ychunk * 100);

                for (int xchunk = 0; xchunk * 100 < w_imorig; xchunk++) {
                    int desty   = xchunk * 100;
                    int regionw = Math.min(100, w_imorig - xchunk * 100);

                    g.drawRegion(im, xchunk * 100, ychunk * 100, regionw, regionh, transform, destx, desty,
                                 Graphics.TOP | Graphics.LEFT);
                }
            }

        } else if (orientation == ORIENT_LEFT) {
            newImage = Image.createImage(h_imorig, w_imorig);
            Graphics g = newImage.getGraphics();
            transform = Sprite.TRANS_ROT270;

            for (int ychunk = 0; ychunk * 100 < h_imorig; ychunk++) {
                int destx = ychunk * 100;
                int regionh = Math.min(100, h_imorig - ychunk * 100);

                for (int xchunk = 0; xchunk * 100 < w_imorig; xchunk++) {
                    int desty   = Math.max(0, w_imorig - xchunk * 100 - 100);
                    int regionw = Math.min(100, w_imorig - xchunk * 100);

                    g.drawRegion(im, xchunk * 100, ychunk * 100, regionw, regionh, transform, destx, desty,
                                 Graphics.TOP | Graphics.LEFT);
                }
            }
            
        } else if (orientation == ORIENT_DOWN) {  
            newImage = Image.createImage(w_imorig, h_imorig);
            Graphics g = newImage.getGraphics();
            transform = Sprite.TRANS_ROT180;

            for (int ychunk = 0; ychunk*100 < h_imorig; ychunk++) {
                int desty = Math.max(0, h_imorig - ychunk*100 - 100);
                int regionh = Math.min(100, h_imorig - ychunk*100);

                for (int xchunk = 0; xchunk*100 < w_imorig; xchunk++) {
                    int destx   = Math.max(0, w_imorig - xchunk*100 - 100);
                    int regionw = Math.min(100, w_imorig - xchunk*100);

                    g.drawRegion(im, xchunk*100, ychunk*100, regionw, regionh, transform, destx, desty,
                                 Graphics.TOP | Graphics.LEFT);
                }
            }

        } else { // ORIENT_UP
            return Image.createImage(imSrc);
        }
        
        return newImage;        
    }

    
    public void rotateImage(int direction) {
        
        // uses different method for 100% zoom rotate and zoom out rotate
        // for better memory use during 100% zoom, and zoom out rotate 
        // rotates zoomed image, and handles Out of Memory error
        // without losing zoomed image (resetting it back to imoriginal)
        
        // 100% zoom rotate
        if(zoomlevel == 1) {
            rotateImageInPlace(direction);
            return;
        }
        
        // zoomed rotate
        Image newImage;

        try {
            newImage = Image.createImage(h, w);
        } catch (OutOfMemoryError e) {
            newImage = null;
            midlet.alert("Out of Memory! [roti]");
            System.gc();
            return;
        }

        Graphics g = newImage.getGraphics();

//      public void drawRegion(Image src,
//                             int x_src,
//                             int y_src,
//                             int width,
//                             int height,
//                             int transform,
//                             int x_dest,
//                             int y_dest,
//                             int anchor)
        // out of memory if rotate all at one shot, so draw in 100x100 regions
        int transform;

        if (direction == ROTATE_CW) {
            transform = Sprite.TRANS_ROT90;

            for (int ychunk = 0; ychunk * 100 < h; ychunk++) {
                int destx = Math.max(0, h - ychunk * 100 - 100);

                for (int xchunk = 0; xchunk * 100 < w; xchunk++) {
                    int desty   = xchunk * 100;
                    int regionh = Math.min(100, h - ychunk * 100);
                    int regionw = Math.min(100, w - xchunk * 100);

                    g.drawRegion(im, xchunk * 100, ychunk * 100, regionw, regionh, transform, destx, desty,
                                 Graphics.TOP | Graphics.LEFT);
                }
            }

            // rotate center point (cx, cy) -> (h-cy, cx).  -> top left ref  (x-dispWidth/2, y-dispHeight/2) 
            // x+dispWidth/2, y+dispHeight/2 -rot-> h-(y+dispHeight/2), x+dispWidth/2 -TL-> 
            // h-y-(dispHeight+dispWidth)/2, x+(dispWidth-dispHeight)/2 
            int x0 = -x;
            int y0 = -y;
            x = h - y0 - (dispHeight+dispWidth)/2;         
            y = x0 + (dispWidth-dispHeight)/2;
            x = -x;
            y = -y;
            orientation = (orientation+1)%4;
            
        } else if (direction == ROTATE_CCW) {
            transform = Sprite.TRANS_ROT270;

            for (int ychunk = 0; ychunk * 100 < h; ychunk++) {
                int destx = ychunk * 100;

                for (int xchunk = 0; xchunk * 100 < w; xchunk++) {
                    int desty   = Math.max(0, w - xchunk * 100 - 100);
                    int regionh = Math.min(100, h - ychunk * 100);
                    int regionw = Math.min(100, w - xchunk * 100);

                    g.drawRegion(im, xchunk * 100, ychunk * 100, regionw, regionh, transform, destx, desty,
                                 Graphics.TOP | Graphics.LEFT);
                }
            }
            int x0 = -x;
            int y0 = -y;
            y = w - x0 - (dispHeight+dispWidth)/2;         
            x = y0 + (dispHeight-dispWidth)/2;
            x = -x;
            y = -y;
            orientation = (orientation-1)%4;
            
        } else {
            return;
        }

        im = newImage;
        w  = im.getWidth();
        h  = im.getHeight();
        
        newImage = null;
        System.gc();
        
        repaint();
    }

    protected void sizeChanged(int w, int h) {
        super.sizeChanged(w, h);
        dispHeight = h;
        dispWidth  = w;
        xscroll    = dispWidth / 3;
        yscroll    = dispHeight / 3;
    }

}