/*-
 * #%L
 * A nice project implementing an OMERO connection with ImageJ
 * %%
 * Copyright (C) 2021 EPFL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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
 * parameters ("annotation") : ImageJ input parameters declaration
 */
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>open OMERO dataset of multiresolution images in BDV")
public class OpenOmeroDataset implements Command {

    @Parameter(label = "OMERO host")
    String host;

    @Parameter(label = "Enter your gaspar username")
    String username;

    @Parameter(label = "Enter your gaspar password", style = "password", persist = false)
    String password;

    @Parameter(label = "Enter the first ID of your dataset")
    long imageID1;

    @Parameter(label = "Enter the last ID of your dataset")
    long imageID2;

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
            System.out.println( "Connection to OMERO.....");
            Gateway gateway =  OmeroTools.omeroConnect(host, port, username, password);
            System.out.println( "Session active : "+gateway.isConnected() );
            SecurityContext ctx = getSecurityContext(gateway);

            for(long imageID=imageID1; imageID<imageID2+1; imageID++){
                System.out.println("New image from Dataset");
                //create a new opener and modify it
                OmeroSourceOpener opener = new OmeroSourceOpener()
                        .imageID(imageID)
                        .gateway(gateway)
                        .securityContext(ctx)
                        .micrometer()
                        .displayInSpace(true)
                        .create();
                System.out.println("Opener has been created");
                //(new Gson()).toJson(opener);
                //System.out.println(new Gson().toJson(opener));

                //
                sacs = new SourceAndConverter[opener.getSizeC()];

                for (int c = 0; c < opener.getSizeC(); c++) {
                    // create the right source and convertor depending on the image type
                    sacs[c] = opener.getSourceAndConvertor(c);
                }

                // give the sources to the sacService (BDV source manager)
                for (SourceAndConverter sac : sacs) {
                    sacService.register(sac);
                }

                MetadataFacility metadata = gateway.getFacility(MetadataFacility.class);
                List<ChannelData> channelMetadata = metadata.getChannelData(ctx, imageID);

                for (int i = 0; i < sacs.length; i++) {
                    Length emWv = channelMetadata.get(i).getEmissionWavelength(UnitsLength.NANOMETER);
                    Length exWv = channelMetadata.get(i).getExcitationWavelength(UnitsLength.NANOMETER);

                    //If EmissionWavelength (or ExcitationWavelength) is contained in the image metadata, convert it to RGB colors for the different channels
                    //Otherwise, put red, green and blue
                    if (emWv != null) {
                        new ColorChanger(sacs[i], getRGBFromWavelength((int) emWv.getValue())).run();
                    } else if (exWv != null) {
                        new ColorChanger(sacs[i], getRGBFromWavelength((int) exWv.getValue())).run();
                    } else {
                        //new ColorChanger(sacs[i], new ARGBType(ARGBType.rgba(255*(1-(i%3)), 255*(1-((i+1)%3)), 255*(1-((i+2)%3)), 255 ))).run();
                        //If no emission nor excitation colors, display RGB
                        new ColorChanger(sacs[i], new ARGBType(ARGBType.rgba(255 * (Math.ceil(((i + 2) % 3) / 2)), 255 * (Math.ceil(((i + 1) % 3) / 2)), 255 * (Math.ceil(((i + 3) % 3) / 2)), 255))).run();
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

        ij.command().run(OpenOmeroDataset.class, true).get();

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
