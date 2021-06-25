package ch.epfl.biop.ij2command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import omero.RType;
import omero.cmd.CmdCallbackI;
import omero.cmd.OriginalMetadataRequest;
import omero.cmd.OriginalMetadataResponse;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.ChannelData;
import omero.gateway.model.ImageAcquisitionData;
import omero.gateway.model.PixelsData;
import omero.model.Length;
import omero.model.PlaneInfo;
import omero.model.enums.UnitsLength;
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
import java.util.Map;

import static ch.epfl.biop.ij2command.OmeroTools.getSecurityContext;
import static ch.epfl.biop.utils.MetadataUtils.getRGBFromWavelength;

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

            MetadataFacility metadata = gateway.getFacility(MetadataFacility.class);
            List<ChannelData> channelMetadata = metadata.getChannelData(ctx, imageID);

            for (int i=0;i<sacs.length;i++) {
                Length wv = channelMetadata.get(i).getEmissionWavelength(UnitsLength.NANOMETER);
                //Length wv = channelMetadata.get(i).getExcitationWavelength(UnitsLength.NANOMETER);

                //If EmissionWavelength is contained in the image metadata, convert it to RGB colors for the different channels
                //Otherwise, put arbitrary colors
                if (wv != null){
                    new ColorChanger(sacs[i], getRGBFromWavelength((int)wv.getValue())).run();
                } else {
                    new ColorChanger(sacs[i], new ARGBType(ARGBType.rgba(255*(i%8), 255*((i+1)%2), 255*(i%2), 255 ))).run();
                }
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


            ImageAcquisitionData acquisitionData = metadata.getImageAcquisitionData(ctx,imageID);
            Length x = acquisitionData.getPositionX(omero.model.enums.UnitsLength.MICROMETER);
            if (x != null)
                System.out.println("x="+x);
            Length y = acquisitionData.getPositionX(omero.model.enums.UnitsLength.MICROMETER);
            if (y != null)
                System.out.println("y="+y);

            /*String field = "Image|ATLConfocalSettingDefinition|StagePosX";
            OriginalMetadataRequest omr = new OriginalMetadataRequest(imageID);
            CmdCallbackI cmd = gateway.submit(ctx, omr);
            OriginalMetadataResponse rsp = (OriginalMetadataResponse) cmd.loop(5, 500);

            Map<String, omero.RType> gm = rsp.seriesMetadata;
            String posx = gm.get(field).toString();
            omero.RType type = gm.get(field);

            System.out.println("pos x ="+type.ice_id());*/

            PixelsData pixels = OmeroTools.getPixelsDataFromOmeroID(imageID, gateway, ctx);

            List<omero.model.IObject> objectinfos = gateway.getQueryService(ctx)
                    .findAllByQuery("select info from PlaneInfo as info " +
                                    "join fetch info.deltaT as dt " +
                                    "join fetch info.exposureTime as et " +
                                    "where info.pixels.id=" + pixels.getId(),
                            null);

            System.out.println("taille object infos " + objectinfos.size());

            PlaneInfo planeinfo = (PlaneInfo)(objectinfos.get(0));

            System.out.println("plane info : " + planeinfo.getPositionX());


            // End of session
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                //fail
                System.out.println( "Session active : "+gateway.isConnected() );
                gateway.disconnect();
                System.out.println("Gateway disconnected");
            }));

        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
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
        //ij.command().run(RawPixelsfromSource.class, true, "imageID", 18317).get();

        //small vsi fluo
        //ij.command().run(RawPixelsfromSource.class, true, "imageID",14746);

        //time lapse: 4677

    }

}