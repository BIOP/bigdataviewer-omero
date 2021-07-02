
package ch.epfl.biop.omero.imageloader;

import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.imageloader.*;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.Dimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import ome.units.UNITS;
import omero.gateway.model.ChannelData;
import org.apache.commons.io.FilenameUtils;
import spimdata.util.Displaysettings;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper.getColorFromWavelength;

/**
 * Converting BioFormats structure into an Xml Dataset, compatible for BigDataViewer and FIJI BIG Plugins
 * Limitation
 * Omero openers are considered as Tiles, no Illumination or Angle is considered
 *
 * @author nicolas.chiaruttini@epfl.ch, BIOP, EPFL 2020
 */

public class OmeroToSpimData {

    //protected static Logger logger = LoggerFactory.getLogger(OmeroToSpimData.class);

    /*
    private int getChannelId(IMetadata omeMeta, int iChannel, boolean isRGB) {
        BioFormatsMetaDataHelper.BioformatsChannel channel = new BioFormatsMetaDataHelper.BioformatsChannel(omeMeta, iSerie, iChannel, false);
        if (!channelToId.containsKey(channel)) {
            // No : add it in the channel hashmap
            channelToId.put(channel,channelCounter);
            logger.debug("New Channel for series "+iSerie+", channel "+iChannel+", set as number "+channelCounter);
            channelIdToChannel.put(channelCounter, new Channel(channelCounter));
            channelCounter++;
        } else {
            logger.debug("Channel for series "+iSerie+", channel "+iChannel+", already known.");
        }
        int idChannel = channelIdToChannel.get(channelToId.get(channel)).getId();
        return idChannel;
    }
*/

    int viewSetupCounter = 0;
    int openerIdxCounter = 0;
    int maxTimepoints = -1;
    int channelCounter = 0;

    //Map<Integer,Channel> channelIdToChannel = new HashMap<>();
    //Map<BioFormatsMetaDataHelper.BioformatsChannel,Integer> channelToId = new HashMap<>();
    //Map<Integer,Integer> fileIdxToNumberOfSeries = new HashMap<>();
    //Map<Integer, SeriesTps> fileIdxToNumberOfSeriesAndTimepoints = new HashMap<>();
    Map<Integer, OpenerIdxChannel> viewSetupToOpenerIdxChannel = new HashMap<>();

    public AbstractSpimData getSpimDataInstance(List<OmeroSourceOpener> openers) {
        openers.forEach(o -> o.ignoreMetadata()); // necessary for spimdata
        viewSetupCounter = 0;
        openerIdxCounter = 0;
        maxTimepoints = -1;
        channelCounter = 0;

        // No Illumination
        Illumination dummy_ill = new Illumination(0);
        // No Angle
        Angle dummy_ang = new Angle(0);
        // No Tile
        Tile dummy_tile = new Tile(0);
        // No Channel
        Channel dummy_channel = new Channel(0);
        // Many View Setups
        List<ViewSetup> viewSetups = new ArrayList<>();

        try {
            for (int openerIdx=0; openerIdx<openers.size(); openerIdx++) {
                FileIndex fi = new FileIndex(openerIdx);
                OmeroSourceOpener opener = openers.get(openerIdx);
                //openerIdxCounter++;
                /*if (omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue() > maxTimepoints) {
                    maxTimepoints = omeMeta.getPixelsSizeT(iSerie).getNumberValue().intValue();
                }*/
                String imageName = opener.getImageName();
                Dimensions dims = opener.getDimensions();
                //logger.debug("X:"+dims.dimension(0)+" Y:"+dims.dimension(1)+" Z:"+dims.dimension(2));
                VoxelDimensions voxDims = opener.getVoxelDimensions();
                List<ChannelData> channelMetadata = opener.getChannelMetadata();
                // Register Setups (one per channel and one per timepoint)
                for (int channelIdx=0; channelIdx<opener.getSizeC(); channelIdx++) {
                    String channelName = channelMetadata.get(channelIdx).getChannelLabeling();
                    String setupName = imageName + "-" + channelName;
                    //logger.debug(setupName);
                    ViewSetup vs = new ViewSetup(
                            viewSetupCounter,
                            setupName,
                            dims,
                            voxDims,
                            dummy_tile,
                            dummy_channel,
                            dummy_ang,
                            dummy_ill);
                    vs.setAttribute(fi);
                    /*
                    // Attempt to set color
                    Displaysettings ds = new Displaysettings(viewSetupCounter);
                    ds.min = 0;
                    ds.max = 255;

                    ds.isSet = false;

                    // ----------- Color
                    ARGBType color = BioFormatsMetaDataHelper.getColorFromMetadata(omeMeta, iSerie, iCh);

                    if (color != null) {
                        ds.isSet = true;
                        ds.color = new int[]{
                                ARGBType.red(color.get()),
                                ARGBType.green(color.get()),
                                ARGBType.blue(color.get()),
                                ARGBType.alpha(color.get())};
                    }

                    vs.setAttribute(ds);
                    */

                    viewSetups.add(vs);
                    viewSetupToOpenerIdxChannel.put(viewSetupCounter, new OpenerIdxChannel(openerIdx,channelIdx));
                    viewSetupCounter++;
                }
            }

            // ------------------- BUILDING SPIM DATA
            ArrayList<String> inputFilesArray = new ArrayList<>();
            for (OmeroSourceOpener opener:openers) {
                inputFilesArray.add(opener.getDataLocation());
            }
            List<TimePoint> timePoints = new ArrayList<>();
            IntStream.range(0,maxTimepoints).forEach(tp -> timePoints.add(new TimePoint(tp)));

            final ArrayList<ViewRegistration> registrations = new ArrayList<>();

            List<ViewId> missingViews = new ArrayList<>();
            for (int openerIdx=0; openerIdx<openers.size(); openerIdx++) {

                //logger.debug("Number of Series : " + memo.getSeriesCount());

                // Need to set view registrations : identity ? how does that work with the one given by the image loader ?
                //IntStream series = IntStream.range(0, nSeries);

                final int nTimepoints = openers.get(openerIdx).getSizeT();
                /*AffineTransform3D rootTransform = BioFormatsMetaDataHelper.getSeriesRootTransform(
                        openers.get(openerIdx).u,
                        openers.get(openerIdx).positionPreTransformMatrixArray, //AffineTransform3D positionPreTransform,
                        openers.get(openerIdx).positionPostTransformMatrixArray, //AffineTransform3D positionPostTransform,
                        openers.get(openerIdx).positionReferenceFrameLength,
                        openers.get(openerIdx).positionIsImageCenter, //boolean positionIsImageCenter,
                        openers.get(openerIdx).voxSizePreTransformMatrixArray, //voxSizePreTransform,
                        openers.get(openerIdx).voxSizePostTransformMatrixArray, //AffineTransform3D voxSizePostTransform,
                        openers.get(openerIdx).voxSizeReferenceFrameLength, //null, //Length voxSizeReferenceFrameLength,
                        openers.get(openerIdx).axesOfImageFlip // axesOfImageFlip
                );*/
                AffineTransform3D rootTransform = new AffineTransform3D();
                final int oIdx = openerIdx;
                timePoints.forEach(iTp -> {
                    viewSetupToOpenerIdxChannel
                        .keySet()
                        .stream()
                        .filter(viewSetupId -> (viewSetupToOpenerIdxChannel.get(viewSetupId).openerIdx == oIdx))
                        .forEach(viewSetupId -> {
                            if (iTp.getId()<nTimepoints) {

                                registrations.add(new ViewRegistration(iTp.getId(), viewSetupId, rootTransform));
                            } else {
                                missingViews.add(new ViewId(iTp.getId(), viewSetupId));
                            }
                        });
                });

            }

            SequenceDescription sd = new SequenceDescription( new TimePoints( timePoints ), viewSetups , null, new MissingViews(missingViews));
            sd.setImgLoader(new OmeroImageLoader(openers,sd,openers.get(0).getNumFetcherThreads(), openers.get(0).getNumPriorities()));

            final SpimData spimData = new SpimData( null, sd, new ViewRegistrations( registrations ) );
            return spimData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static AbstractSpimData getSpimData(List<OmeroSourceOpener> openers) {
        return new OmeroToSpimData().getSpimDataInstance(openers);
    }

    public static AbstractSpimData getSpimData(OmeroSourceOpener opener) {
        ArrayList<OmeroSourceOpener> singleOpenerList = new ArrayList<>();
        singleOpenerList.add(opener);
        return new OmeroToSpimData().getSpimData(singleOpenerList);
    }

    public static AbstractSpimData getSpimData(File f) {
        OmeroSourceOpener opener = getDefaultOpener(f.getAbsolutePath());
        return getSpimData(opener);
    }

    public static AbstractSpimData getSpimData(File[] files) {
        ArrayList<OmeroSourceOpener> openers = new ArrayList<>();
        for (File f:files) {
            openers.add(getDefaultOpener(f.getAbsolutePath()));
        }
        return new OmeroToSpimData().getSpimData(openers);
    }

    public static OmeroSourceOpener getDefaultOpener(String dataLocation) {
        return OmeroSourceOpener.getOpener().location(dataLocation);
    }



}
