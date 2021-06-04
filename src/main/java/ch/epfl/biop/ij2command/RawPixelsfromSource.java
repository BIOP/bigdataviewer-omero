package ch.epfl.biop.ij2command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
import com.google.gson.Gson;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

import java.util.ArrayList;
import java.util.List;

import static ch.epfl.biop.ij2command.OmeroTools.getSecurityContext;

/**
 * Command for displaying an OMERO image (raw pixels) in 3D in BDV
 *
 * @parameters ("annotation") : ImageJ input parameters declaration
 */
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>open OMERO multiresolution image in BDV")
public class RawPixelsfromSource implements Command {

    @Parameter(label = "OMERO host")
    String host;

    @Parameter(label = "Enter your gaspar username")
    String username;

    @Parameter(label = "Enter your gaspar password", style = "password", persist = false)
    String password;

    @Parameter(label = "Enter the ID of your OMERO image")
    long imageID;

    @Parameter
    boolean autocontrast;

    @Parameter
    boolean show;

    static int port = 4064;

    /**
     * Command Output
     */
    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter[] sacs;

    /**
     * BDV Service for managing all BDV Sources
     * https://github.com/bigdataviewer/bigdataviewer-playground
     */
    @Parameter
    SourceAndConverterService sacService;

    /**
     * BDV Service for display
     * https://github.com/bigdataviewer/bigdataviewer-playground
     */
    @Parameter
    SourceAndConverterBdvDisplayService sacDisplayService;

    @Override
    public void run() {
        try {
            Gateway gateway =  OmeroTools.omeroConnect(host, port, username, password);
            System.out.println( "Session active : "+gateway.isConnected() );
            SecurityContext ctx = getSecurityContext(gateway);

            //create a new opener and modify it
            OmeroSourceOpener opener = new OmeroSourceOpener()
                    .imageID(imageID)
                    .gateway(gateway)
                    .securityContext(ctx)
                    .millimeter()
                    .create();

            //(new Gson()).toJson(opener);
            //System.out.println(new Gson().toJson(opener));

            //
            sacs = new SourceAndConverter[opener.getSizeC()];

            for (int c=0; c<opener.getSizeC(); c++) {
                // create the right source and convertor depending on the image type
                sacs[c] = opener.getSourceAndConvertor(c);
            }

            // give the sources to the sacService (BDV source manager)
            for (SourceAndConverter sac:sacs){
                sacService.register(sac);
            }

            for (int i=0;i<sacs.length;i++) {
                new ColorChanger(sacs[i], new ARGBType(ARGBType.rgba(255*(i%8), 255*((i+1)%2), 255*(i%2), 255 ))).run();
                //handle autocontrast option
                if (autocontrast) {
                    new BrightnessAutoAdjuster(sacs[i], 0).run();
                }
            }

            //handle show option
            if (show) {
                SourceAndConverterServices.getBdvDisplayService().show(sacDisplayService.getActiveBdv(), sacs);
                //adjust the viewing window in BDV to the image
                (new ViewerTransformAdjuster(sacDisplayService.getActiveBdv(), this.sacs[0])).run();
            }

            // End of session
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                //fail
                System.out.println( "Session active : "+gateway.isConnected() );
                gateway.disconnect();
                System.out.println("Gateway disconnected");
            }));

        } catch (Exception e) {
            e.printStackTrace();
        }
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

        ij.command().run(RawPixelsfromSource.class, true).get();

        //vsi fluo
        //ij.command().run(RawPixelsfromSource.class, true, "imageID",3713).get();
        //ij.command().run(RawPixelsfromSource.class, true, "imageID",24601).get();

        //IJ.run("BDV - Show Sources (new Bdv window)", "autocontrast=true adjustviewonsource=true is2d=true windowtitle=BDV interpolate=false ntimepoints=1 projector=[Sum Projector]");

        //lif 4 channels, (1024 1024)
        //ij.command().run(RawPixelsfromSource.class, true, "imageID",24720);
        //ij.command().run(RawPixelsfromSource.class, true, "imageID",18024).get();

        //small vsi fluo
        //ij.command().run(RawPixelsfromSource.class, true, "imageID",14746);

        //time lapse: 4677

    }

}