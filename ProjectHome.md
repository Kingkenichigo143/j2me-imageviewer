A useful MIDlet to view locally stored images at full resolution. It can only display image types that your phone's java virtual machine (KVM) natively supports; at minimum all KVM support PNG images; support for other image types such as JPG, GIF, BMP varies by phone.  Displayable image size depends on available KVM memory. Transform operations (rotate, zoom) require more memory. (For example: My KVM reports 11MB available memory can display but cannot rotate 1850x1400 images. Displays and rotate 1650x1250 image.)

Hardware Requirement: CLDC 1.1 + MIDP 2.0

Features:
**Tiny <22K.  Will run on most non-smart phones with java support.** Local file system access (jsr75)
**Pan/Scroll** Full screen
**1:1 / 100% image view** Zoom out: 50%, 33%, 25%.
**Rotate by 90 degree. Centered on current image pan location.** MRU : uses RMS to remember most recently accfessed folder and images
**Info : basic info page showing image dimensions, folder, free KVM memory** Fast (as practical)
**Handles Out of Memory without crashing** optional acceleration sensor auto-rotate (as tested and calibrated on my phone)(jsr256) use j2me-imageviewer-autorotate.jad to install

Key Commands: {OK} Toggle full screen. {Arrows/4/6/8/2} Pan/scroll. {#/`*`} Zoom. {0} Rotate

credits: Inspired by and framework taken from <a href='http://code.google.com/p/baigosviewer/'>baigosviewer</a>.  j2me-imageviewer fills out all the functionality, bug fixes and adds bells-and-whistles.
GPL <a href='http://findicons.com/icon/238004/gwenview?width=32'>eye-icon</a> by <a href='http://www.oxygen-icons.org/'>Oxygen Team</a>.

developed on LG Java ME SDK and netbeans 7.0, tested on LG GD570 dLite flip phone.

links to <a href='http://code.google.com/p/j2me-imageviewer/source/browse/#svn%2Ftrunk'>**SOURCE</a> &**<a href='http://code.google.com/p/j2me-imageviewer/downloads/list'>BINARY</a> FILES