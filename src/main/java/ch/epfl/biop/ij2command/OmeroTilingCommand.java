package ch.epfl.biop.ij2command;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.VolatileViews;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageProcessor;
import loci.plugins.LociImporter;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.log.SimpleLogger;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.VersionUtils;


@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Omero Tiling")
public class OmeroTilingCommand implements Command {

    @Parameter(label = "OMERO host")
    String host;

    @Parameter(label = "Enter your gaspar username")
    String username;

    @Parameter(label = "Enter your gaspar password", style = "password", persist = false)
    String password;

    @Parameter(label = "Enter the ID of your OMERO image")
    long imageID;

    static int port = 4064;


    int index = 1;

    @Override
    public void run() {
        System.out.println(VersionUtils.getVersion(BdvHandle.class));
        // Run the function
        // Connect to Omero
        // https://downloads.openmicroscopy.org/omero/5.4.10/api/omero/gateway/Gateway.html
        RandomAccessibleInterval volatilerandomAccessible = OmeroTools.openRandomAccessibleInterval(host, username, password, imageID, false);

        BdvStackSource bss = BdvFunctions.show(volatilerandomAccessible,"Tiling");
        bss.setDisplayRange(0, 1500);
        /*BdvOptions bdvOptions = new BdvOptions();
        bdvOptions.addTo(bss.getBdvHandle());
        AffineTransform3D transform3D = new AffineTransform3D();
        transform3D.rotate(2,Math.PI/2.0);
        bdvOptions.sourceTransform(transform3D);
        bss = BdvFunctions.show(volatilerandomAccessible,"Tiling",bdvOptions);
        */

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


