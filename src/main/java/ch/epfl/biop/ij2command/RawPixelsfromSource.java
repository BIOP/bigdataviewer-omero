package ch.epfl.biop.ij2command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
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


//New class for displaying an OMERO image (raw pixels) in 3D in BDV
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>display raw pixels in 3D")
public class RawPixelsfromSource implements Command {

    @Parameter(label = "OMERO host")
    String host;

    @Parameter(label = "Enter your gaspar username")
    String username;

    @Parameter(label = "Enter your gaspar password", style = "password", persist = false)
    String password;

    @Parameter(label = "Enter the ID of your OMERO image")
    long imageID;

    static int port = 4064;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter[] sacs;

    @Parameter
    SourceAndConverterService sacService;

    @Parameter
    SourceAndConverterBdvDisplayService sacDisplayService;

    @Parameter
    boolean autocontrast;

    @Parameter
    boolean show;

    @Override
    public void run() {
        // https://downloads.openmicroscopy.org/omero/5.4.10/api/omero/gateway/Gateway.html
        try {
            Gateway gateway =  OmeroTools.omeroConnect(host, port, username, password);
            System.out.println( "Session active : "+gateway.isConnected() );
            SecurityContext ctx = getSecurityContext(gateway);

            OmeroSourceOpener opener = new OmeroSourceOpener()
                    .imageID(imageID)
                    .gateway(gateway)
                    .securityContext(ctx)
                    .millimeter()
                    .create();

            sacs = new SourceAndConverter[opener.getSizeC()];

            for (int c=0; c<opener.getSizeC(); c++) {
                sacs[c] = opener.getSourceAndConvertor(c);
            }

            List<SourceAndConverter<?>> sacsList = new ArrayList<>();

            for (SourceAndConverter sac:sacs){
                sacsList.add(sac);
                sacService.register(sac);
            }

            for (int i=0;i<sacs.length;i++) {
                new ColorChanger(sacs[i], new ARGBType(ARGBType.rgba(255*(i%8), 255*((i+1)%2), 255*(i%2), 255 ))).run();
                if (autocontrast) {
                    new BrightnessAutoAdjuster(sacs[i], 0).run();
                }
                if (show) {
                    SourceAndConverterServices.getSourceAndConverterDisplayService().show(sacDisplayService.getActiveBdv(), sacs);
                }
            }

            if (show) {
                (new ViewerTransformAdjuster(sacDisplayService.getActiveBdv(), this.sacs[0])).run();
            }

            // End of session
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
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

        //ij.command().run(RawPixelsfromSource.class, true);

        //vsi fluo
        ij.command().run(RawPixelsfromSource.class, true, "imageID",3713).get();
        //ij.command().run(RawPixelsfromSource.class, true, "imageID",24601).get();

        //IJ.run("BDV - Show Sources (new Bdv window)", "autocontrast=true adjustviewonsource=true is2d=true windowtitle=BDV interpolate=false ntimepoints=1 projector=[Sum Projector]");

        //lif 4 channels, (1024 1024)
        //ij.command().run(RawPixelsfromSource.class, true, "imageID",24601);

        //small vsi fluo
        //ij.command().run(RawPixelsfromSource.class, true, "imageID",14746);


    }

}