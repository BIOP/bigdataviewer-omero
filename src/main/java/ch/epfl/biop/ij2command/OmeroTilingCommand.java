package ch.epfl.biop.ij2command;

import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import loci.plugins.LociImporter;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.model.ExperimenterData;
import omero.model.IObject;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.IOException;


//import os.*;
//import os.path.*;

import ij.*;
import ij.IJ;

//import omero.gateway.LoginCredentials
//import omero.gateway.Gateway

import omero.gateway.*;
import omero.gateway.facility.BrowseFacility;

import omero.log.SimpleLogger;
import ome.formats.importer.*;
import ome.formats.importer.cli.*;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;

//Nouvelle copie

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.VolatileViews;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import net.imglib2.*;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.VersionUtils;

import java.util.function.Consumer;



@Plugin(type = Command.class, menuPath = "Plugins>BIOP>OmeroTiling")
public class OmeroTilingCommand implements Command {

    @Parameter
    UIService uiService;

    @Parameter
    PlatformService ps;

    @Parameter(label = "OMERO host")
    String host;

    @Parameter(label = "Enter your gaspar username")
    String username;

    @Parameter(label = "Enter your gaspar password", style = "password", persist = false)
    String password;

    @Parameter(label = "Enter the ID of your OMERO image")
    long imageID;

    static int port = 4064;

   // @Parameter
   // ImagePlus image;

    int index = 1;

    @Override
    public void run() {
        System.out.println(VersionUtils.getVersion(BdvHandle.class));
        // Run the function
        // Connect to Omero
        // https://downloads.openmicroscopy.org/omero/5.4.10/api/omero/gateway/Gateway.html
        try {
            Gateway gateway = omeroConnect(host, port, username, password);
            System.out.println( "Session active : "+gateway.isConnected() );
            openImagePlus(host,username,password,imageID);
            System.out.println( "Disconnecting...");
            //gateway.disconnect();
            System.out.println( "Session active : "+gateway.isConnected() );
        }
        catch(Exception e) { e.printStackTrace();
        }

        ImagePlus image = WindowManager.getCurrentImage();
        //image.show();
        // Read image dimensions and set total dimensions of the tiled image accordingly
        long[] total_dim = new long[2];
        total_dim[0] = image.getWidth()*image.getNSlices();
        total_dim[1] = image.getHeight();
        System.out.println("xdim : " + total_dim[0]+" ydim : " +total_dim[1]);

        // Create cached image factory of Type Byte
        ReadOnlyCachedCellImgOptions options = new ReadOnlyCachedCellImgOptions();
        // Put cell dimensions to image width and height
        options = options.cellDimensions(image.getWidth(),image.getHeight());
        final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory(options);

        UnsignedShortType t = new UnsignedShortType();

        CellLoader<UnsignedShortType> loader = new CellLoader<UnsignedShortType>(){
            @Override
            public void load(SingleCellArrayImg<UnsignedShortType, ?> singleCellArrayImg) throws Exception {

                ImageProcessor ip = image.getStack().getProcessor(index);

                long[] positions = new long[2];
                Cursor<UnsignedShortType> cursor = singleCellArrayImg.localizingCursor();

                final long cellOffset = - (index-1)*image.getWidth();


                // move through pixels until there is no pixel left in this cell
                while (cursor.hasNext())
                {
                    // move the cursor forward by one pixel
                    cursor.fwd();
                    //get the current position
                    cursor.localize(positions);
                    long px = positions[0] + cellOffset;
                    long py = positions[1];
                    //get pixel value of the input image (from stack) at pos (px,py) and copy it to the current cell at the same position
                    cursor.get().set(ip.getPixel((int) px,(int) py));
                }
                index = index+1;
            }
        };
        RandomAccessibleInterval<UnsignedShortType> randomAccessible = factory.create(total_dim, t,loader);
        //ask if pixel has already been loaded or not
        RandomAccessibleInterval volatilerandomAccessible = VolatileViews.wrapAsVolatile(randomAccessible);
        BdvStackSource bss = BdvFunctions.show(volatilerandomAccessible,"Tiling");
        bss.setDisplayRange(0, 1500);
        BdvOptions bdvOptions = new BdvOptions();
        bdvOptions.addTo(bss.getBdvHandle());
        AffineTransform3D transform3D = new AffineTransform3D();
        transform3D.rotate(2,Math.PI/2.0);
        bdvOptions.sourceTransform(transform3D);
        bss = BdvFunctions.show(volatilerandomAccessible,"Tiling",bdvOptions);
    }


    Gateway omeroConnect(String hostname, int port, String userName, String password)throws Exception{
        //Omero Connect with credentials and simpleLogger
        LoginCredentials cred = new LoginCredentials();
        cred.getServer().setHost(hostname);
        cred.getServer().setPort(port);
        cred.getUser().setUsername(userName);
        cred.getUser().setPassword(password);
        SimpleLogger simpleLogger = new SimpleLogger();
        Gateway gateway = new Gateway(simpleLogger);
        gateway.connect(cred);
        return gateway;
    }


    void openImagePlus(String host,String username,String password,long imageID){
        String options = "";
        options += "location=[OMERO] open=[omero:server=";
        options += host;
        options += "\nuser=";
        options += username;
        options += "\npass=";
        options += password;
        //options += "\ngroupID=";
        //options += groupID;
        options += "\niid=";
        options += imageID;
        options += "]";
        //options += " use_virtual_stack";
        //options += " windowless=true ";
        IJ.runPlugIn("loci.plugins.LociImporter",  options);
        LociImporter li = new LociImporter();
    }


    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ij.command().run(OmeroTilingCommand.class, true);
    }

}