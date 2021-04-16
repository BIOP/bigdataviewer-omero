package ch.epfl.biop.ij2command;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.SharedQueue;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.bioformats.bioformatssource.*;
import ch.epfl.biop.omero.omerosource.OmeroSource;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
import jdk.jfr.Unsigned;
import net.imagej.ImageJ;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.ArrayList;
import java.util.Arrays;
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
            SharedQueue cc = new SharedQueue(8,4);
            //PixelsData pixels = OmeroTools.getPixelsDataFromOmeroID(imageID,gateway,ctx);

            BdvStackSource bss = null;
            sacs = new SourceAndConverter[opener.getSizeC()];

            for (int c=0; c<opener.getSizeC(); c++) {
            //for (int c=0; c<1; c++) {
                OmeroSource concreteSource = new OmeroSource(opener,c);
                VolatileBdvSource volatileSource = new VolatileBdvSource(concreteSource,
                        BioFormatsBdvSource.getVolatileOf((NumericType) concreteSource.getType()),
                        cc);
                /*bss = BdvFunctions.show(volatileSource);
                //bss = BdvFunctions.show(source,pixels.getSizeT());

                //add a time slider
                bss.getBdvHandle().getViewerPanel().setNumTimepoints(concreteSource.getSizeT());
                bss.setDisplayRange(0, 255);
                // Color : Random color for each channel
                bss.setColor(new ARGBType(ARGBType.rgba(255*Math.random(),255*Math.random(),255*Math.random(),1)));
                */
                Converter concreteConverter = SourceAndConverterHelper.createConverter(concreteSource);
                Converter volatileConverter = SourceAndConverterHelper.createConverter(volatileSource);

                sacs[c] = new SourceAndConverter(concreteSource,concreteConverter,
                        new SourceAndConverter<>(volatileSource, volatileConverter));

            }
            List<SourceAndConverter<UnsignedShortType>> list = new ArrayList<>();
            for (SourceAndConverter sac:sacs){
                list.add(sac);
            }
            BdvFunctions.show(list,opener.getSizeT(),BdvOptions.options());

            gateway.disconnect();

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
        ij.command().run(RawPixelsfromSource.class, true, "imageID",3713);

        //lif 4 channels, (1024 1024)
        //ij.command().run(RawPixelsfromSource.class, true, "imageID",24601);

        //small vsi fluo
        //ij.command().run(RawPixelsfromSource.class, true, "imageID",14746);


    }

}