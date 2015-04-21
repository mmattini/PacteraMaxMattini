# PacteraMaxMattini
Pactera android test - Max Mattini 20 Apr 2015

Hi,

Below are some assumptions and notes:

* It seems that images displayed from the feed have different sizes. By default, they are displayed in real size.
  When displayed in real size, the UI does not look nice.
  
* However, I added a menu action to change the display of image to be of the same size. The menu action toggles from
  'Real size' to 'Same size' and when pressed the UI is refreshed

* I added an Android Unit Test, to test Json parsing and non-UI related code.

* Current connection timeout is 10 seconds, instead of the default of 30 seconds. Since some of the 
  image URL (e.g http://images.findicons.com/files/icons/662/world_flag/128/flag_of_canada.png) times 
  out during download, I did not want the user to wait for long.

* I added a progress bar to track image download progress. When all images are downloaded (or failed to download) the image 
  progress disappears. 
  Please note that if you don't scroll the list, the progress bar will remain (as the list adapter request to download 
  the image of only visible items).
  
 * I am using AsyncTask to download images. AsynTask uses default thread pool. The default thread pool, depending on 
   Android OS version, is very small.This is why it takes sometime to download all images.
   
  In real life, it is better to use concurrent package to control the thread number used in downloading the images.  
 

Thanks for the opportunity

Please let me know if you have any question

--maX (0405 500 487)
