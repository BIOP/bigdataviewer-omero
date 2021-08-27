package ch.epfl.biop.omero.omerosource;

import IceInternal.Ex;
import bdv.util.volatiles.SharedQueue;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import ch.epfl.biop.bdv.bioformats.bioformatssource.ReaderPool;
import ch.epfl.biop.bdv.bioformats.bioformatssource.VolatileBdvSource;
import ch.epfl.biop.ij2command.OmeroTools;

import loci.formats.*;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.FinalInterval;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import ome.formats.model.IObjectContainerStore;
import ome.units.UNITS;
import ome.units.unit.Unit;
import omero.ServerError;
import omero.api.IMetadataPrx;
import omero.api.IRenderingSettingsPrx;
import omero.api.RawPixelsStorePrx;
import omero.api.ResolutionDescription;
import omero.gateway.SecurityContext;
import omero.gateway.Gateway;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.ChannelData;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.model.*;
import omero.model.enums.UnitsLength;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.epfl.biop.utils.MetadataUtils.getRGBFromWavelength;
import static omero.gateway.model.PixelsData.*;

/**
 * Contains parameters that explain how to open all channel sources from an Omero Image
 * TODO: make proper builder pattern (2 classes: internal builder class in omerosourceopener)
 */
public class OmeroSourceOpener {



    public OmeroSourceOpener() {
    }

    // All serializable fields (fields needed to create the omeroSourceOpener)
    public String dataLocation = null; // URL or File
    public boolean useOmeroXYBlockSize = true; // Block size : use the one defined by Omero

    long omeroImageID;
    public String host;
    // Channels options
    boolean splitRGBChannels = false;
    // Unit used for display
    public UnitsLength u;
    // Size of the blocks
    //public FinalInterval cacheBlockSize = new FinalInterval(new long[]{0, 0, 0}, new long[]{512, 512, 1}); // needs a default size for z
    // Bioformats location fix
    public double[] positionPreTransformMatrixArray;
    public double[] positionPostTransformMatrixArray;
    public ome.units.quantity.Length positionReferenceFrameLength;
    public boolean positionIgnoreBioFormatsMetaData = false;
    // Bioformats voxsize fix
    public boolean voxSizeIgnoreBioFormatsMetaData = false;
    public ome.units.quantity.Length voxSizeReferenceFrameLength;
    public int numFetcherThreads = 2;
    public int numPriorities = 4;

    // All non-serializable fields
    transient SharedQueue cc;
    transient Gateway gateway;
    transient SecurityContext securityContext;
    transient RawPixelsStorePool pool = new RawPixelsStorePool(10, true, this::getNewStore);
    transient int sizeT;
    transient int sizeC;
    transient int nLevels;
    transient double psizeX;
    transient double psizeY;
    transient double psizeZ;
    transient double stagePosX;
    transient double stagePosY;
    transient Map<Integer,int[]> imageSize;
    transient Map<Integer,int[]> tileSize;
    transient long pixelsID;
    transient String imageName;
    transient List<ChannelData> channelMetadata;
    transient boolean displayInSpace;
    transient RenderingDef renderingDef;

    // All get methods
    public int getSizeX(int level) { return this.imageSize.get(level)[0]; }
    public int getSizeY(int level) {
        return this.imageSize.get(level)[1];
    }
    public int getSizeZ(int level) {
        return this.imageSize.get(level)[2];
    }
    public int getTileSizeX(int level){ return this.tileSize.get(level)[0]; }
    public int getTileSizeY(int level){ return this.tileSize.get(level)[1]; }
    public int getSizeT() {
        return this.sizeT;
    }
    public int getSizeC() {
        return this.sizeC;
    }
    public int getNLevels() {
        return this.nLevels;
    }
    public long getPixelsID() {
        return this.pixelsID;
    }

    public double getPixelSizeX() {
        return this.psizeX;
    }
    public double getPixelSizeY(){
        return this.psizeY;
    }
    public double getPixelSizeZ(){
        return this.psizeZ;
    }
    public double getStagePosX() {
        return this.stagePosX;
    }
    public double getStagePosY(){
        return this.stagePosY;
    }

    public String getDataLocation() {
        return dataLocation;
    }
    public String getHost() {
        return host;
    }
    public List<ChannelData> getChannelMetadata() { return channelMetadata; }
    public RenderingDef getRenderingDef() {return renderingDef; }
    public int getNumFetcherThreads() { return numFetcherThreads; }
    public int getNumPriorities() { return numPriorities; }

    public OmeroSourceOpener positionReferenceFrameLength(ome.units.quantity.Length l) {
        this.positionReferenceFrameLength = l;
        return this;
    }

    public OmeroSourceOpener voxSizeReferenceFrameLength(ome.units.quantity.Length l) {
        this.voxSizeReferenceFrameLength = l;
        return this;
    }

    public OmeroSourceOpener useCacheBlockSizeFromOmero(boolean flag) {
        useOmeroXYBlockSize = flag;
        return this;
    }

    public OmeroSourceOpener location(String location) {
        this.dataLocation = location;
        return this;
    }

    //define image ID
    public OmeroSourceOpener imageID(long imageID) {
        this.omeroImageID = imageID;
        return this;
    }

    public OmeroSourceOpener host(String host) {
        this.host = host;
        return this;
    }


    public OmeroSourceOpener displayInSpace(boolean displayInSpace) {
        this.displayInSpace = displayInSpace;
        return this;
    }

    public OmeroSourceOpener splitRGBChannels() {
        splitRGBChannels = true;
        return this;
    }

    //define unit
    public OmeroSourceOpener unit(UnitsLength u) {
        this.u = u;
        return this;
    }

    public OmeroSourceOpener millimeter() {
        this.u = UnitsLength.MILLIMETER;
        return this;
    }

    public OmeroSourceOpener micrometer() {
        this.u = UnitsLength.MICROMETER;
        return this;
    }

    public OmeroSourceOpener nanometer() {
        this.u = UnitsLength.NANOMETER;
        return this;
    }

    // define gateway
    public OmeroSourceOpener gateway(Gateway gateway) {
        this.gateway = gateway;
        return this;
    }

    // define security context
    public OmeroSourceOpener securityContext(SecurityContext ctx) {
        this.securityContext = ctx;
        return this;
    }

    //define num fetcher threads
    public OmeroSourceOpener numFetcherThreads(int numFetcherThreads) {
        this.numFetcherThreads = numFetcherThreads;
        return this;
    }

    //define num fetcher threads
    public OmeroSourceOpener numPriorities(int numPriorities) {
        this.numPriorities = numPriorities;
        return this;
    }


    // define size fields based on omero image ID, gateway and security context

    /**
     * Builder pattern: fills all the omerosourceopener fields that relates to the image to open
     * (i.e image size for all resolution levels..)
     * @return
     * @throws Exception
     */
    public OmeroSourceOpener create() throws Exception {
        //TODO move it to omerosourceopener
        System.out.println("Load PixelsData...");
        PixelsData pixels = OmeroTools.getPixelsDataFromOmeroID(omeroImageID, gateway, securityContext);
        System.out.println("PixelsData loaded!");
        System.out.println("Load RawPixelsStore...");
        RawPixelsStorePrx rawPixStore = gateway.getPixelsStore(securityContext);
        System.out.println("RawPixelsStore loaded!");
        this.cc = new SharedQueue(numFetcherThreads, numPriorities);
        this.pixelsID = pixels.getId();
        rawPixStore.setPixelsId(this.pixelsID, false);
        this.nLevels = rawPixStore.getResolutionLevels();
        this.imageSize = new HashMap<>();
        this.tileSize = new HashMap<>();
        this.imageName = getImageData(omeroImageID, gateway, securityContext).getName();
        this.channelMetadata = gateway.getFacility(MetadataFacility.class).getChannelData(securityContext,omeroImageID);
        this.renderingDef = gateway.getRenderingSettingsService(securityContext).getRenderingSettings(pixelsID);


        //Optimize time if there is only one resolution level because getResolutionDescriptions() is time-consuming
        if(rawPixStore.getResolutionLevels() == 1){
            imageSize.put(0, new int[]{pixels.getSizeX(), pixels.getSizeY(), pixels.getSizeZ()});
            tileSize = imageSize;
        } else {
            System.out.println("Get image size and tile sizes...");
            Instant start = Instant.now();
            ResolutionDescription[] resDesc = rawPixStore.getResolutionDescriptions();
            Instant finish = Instant.now();
            System.out.println("Done! Time elapsed : " + Duration.between(start, finish));
            int tileSizeX = rawPixStore.getTileSize()[0];
            int tileSizeY = rawPixStore.getTileSize()[1];

            for (int level = 0; level < this.nLevels; level++) {
                int[] sizes = new int[3];
                sizes[0] = resDesc[level].sizeX;
                sizes[1] = resDesc[level].sizeY;
                sizes[2] = pixels.getSizeZ();
                int[] tileSizes = new int[2];
                tileSizes[0] = Math.min(tileSizeX, resDesc[rawPixStore.getResolutionLevels() - 1].sizeX);
                tileSizes[1] = Math.min(tileSizeY, resDesc[rawPixStore.getResolutionLevels() - 1].sizeY);
                imageSize.put(level, sizes);
                tileSize.put(level, tileSizes);
            }
        }

        this.sizeT = pixels.getSizeT();
        this.sizeC = pixels.getSizeC();

        //--X and Y stage positions--
        System.out.println("Begin SQL request for OMERO image with ID : " + this.omeroImageID);
        List<IObject> objectinfos = gateway.getQueryService(securityContext)
                .findAllByQuery("select info from PlaneInfo as info " +
                                "join fetch info.deltaT as dt " +
                                "join fetch info.exposureTime as et " +
                                "where info.pixels.id=" + pixels.getId(),
                        null);
        if(objectinfos.size() != 0) {
            //one plane per (c,z,t) combination: we assume that X and Y stage positions are the same in all planes and therefore take the 1st plane
            PlaneInfo planeinfo = (PlaneInfo) (objectinfos.get(0));
            //Convert the offsets in the unit given in the builder
            Length lengthPosX = new LengthI(planeinfo.getPositionX(), this.u);
            Length lengthPosY = new LengthI(planeinfo.getPositionY(), this.u);
            this.stagePosX = lengthPosX.getValue();
            this.stagePosY = lengthPosY.getValue();
        } else {
            this.stagePosX = 0;
            this.stagePosY = 0;
        }
        System.out.println("SQL request completed!");
        //psizes are expressed in the unit given in the builder
        this.psizeX = pixels.getPixelSizeX(this.u).getValue();
        this.psizeY = pixels.getPixelSizeY(this.u).getValue();
        //to handle 2D images
        this.psizeZ = 1;
        Length length = pixels.getPixelSizeZ(this.u);
        if (length != null) {
            this.psizeZ = length.getValue();
        }

        // must close the rawPixStore to free up resources
        rawPixStore.close();
        return this;
    }


    // All space transformation methods
    public OmeroSourceOpener flipPositionXYZ() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(-1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public OmeroSourceOpener flipPositionX() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(-1,1,1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public OmeroSourceOpener flipPositionY() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(1,-1,1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public OmeroSourceOpener flipPositionZ() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(1,1,-1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public OmeroSource<?> createOmeroSource(int channel) throws Exception {
        PixelsData pixels = getPixelsDataFromOmeroID(omeroImageID, gateway, securityContext);
        OmeroSource source;
        //TODO : get pixel type as a field in omerosourceopener
        switch(pixels.getPixelType()){
            case FLOAT_TYPE: source = new OmeroSourceFloat(this, channel);
                break;
            case UINT16_TYPE: source = new OmeroSourceUnsignedShort(this, channel);
                break;
            case UINT8_TYPE: source = new OmeroSourceUnsignedByte(this, channel);
                break;
            case UINT32_TYPE: source = new OmeroSourceUnsignedInt(this, channel);
                break;
            default:
                throw new IllegalStateException("Unsupported pixel type : " + pixels.getPixelType());
        }
        return source;
    }

    public static PixelsData getPixelsDataFromOmeroID(long imageID, Gateway gateway, SecurityContext ctx) throws Exception{
        ImageData image = getImageData(imageID, gateway, ctx);
        PixelsData pixels = image.getDefaultPixels();
        return pixels;

    }

    public static ImageData getImageData(long imageID, Gateway gateway, SecurityContext ctx) throws Exception{
        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        ImageData image = browse.getImage(ctx, imageID);
        return image;
    }

    public SourceAndConverter getSourceAndConvertor(int c) throws Exception {
        // create the right concrete source depending on the image type
        OmeroSource concreteSource = createOmeroSource(c);
        // create the volatile source based on the concrete source
        VolatileBdvSource volatileSource = new VolatileBdvSource(concreteSource,
                BioFormatsBdvSource.getVolatileOf((NumericType) concreteSource.getType()),cc);

        Converter concreteConverter = SourceAndConverterHelper.createConverter(concreteSource);
        Converter volatileConverter = SourceAndConverterHelper.createConverter(volatileSource);

        return new SourceAndConverter(concreteSource,concreteConverter,
                new SourceAndConverter<>(volatileSource, volatileConverter));

    }

    public List<Source> getConcreteAndVolatileSources(int c) throws Exception{
        // create the right concrete source depending on the image type
        OmeroSource concreteSource = createOmeroSource(c);
        // create the volatile source based on the concrete source
        VolatileBdvSource volatileSource = new VolatileBdvSource(concreteSource,
                BioFormatsBdvSource.getVolatileOf((NumericType) concreteSource.getType()),cc);

        List<Source> sources = new ArrayList<>(2);
        sources.add(concreteSource);
        sources.add(volatileSource);
        return sources;

    }

    public ARGBType getChannelColor(int c) throws Exception{

        ChannelBinding cb = renderingDef.getChannelBinding(c);

        /*
        Length emWv = channelMetadata.get(c).getEmissionWavelength(UnitsLength.NANOMETER);
        Length exWv = channelMetadata.get(c).getExcitationWavelength(UnitsLength.NANOMETER);

        //If EmissionWavelength (or ExcitationWavelength) is contained in the image metadata, convert it to RGB colors for the different channels
        //Otherwise, put red, green and blue
        if (emWv != null) {
            return getRGBFromWavelength((int) emWv.getValue());
        } else if (exWv != null) {
            return getRGBFromWavelength((int) exWv.getValue());
        } else {
            //new ColorChanger(sacs[i], new ARGBType(ARGBType.rgba(255*(1-(i%3)), 255*(1-((i+1)%3)), 255*(1-((i+2)%3)), 255 ))).run();
            //If no emission nor excitation colors, display RGB
            return new ARGBType(ARGBType.rgba(255 * (Math.ceil(((c + 2) % 3) / 2)), 255 * (Math.ceil(((c + 1) % 3) / 2)), 255 * (Math.ceil(((c + 3) % 3) / 2)), 255));
        }*/
        return new ARGBType(ARGBType.rgba(cb.getRed().getValue(), cb.getGreen().getValue(), cb.getBlue().getValue(), cb.getAlpha().getValue()));
    }


    /**
     * RawPixelStore supplier method for the RawPixelsStorePool.
     */
    public RawPixelsStorePrx getNewStore() {
        try {
            RawPixelsStorePrx rawPixStore = gateway.getPixelsStore(securityContext);
            rawPixStore.setPixelsId(getPixelsID(), false);
            return rawPixStore;
        } catch (ServerError | DSOutOfServiceException serverError) {
            serverError.printStackTrace();
        }
        return null;
    }

    public AffineTransform3D getSourceTransform(int level) {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        transform.scale(getPixelSizeX()*(double)imageSize.get(0)[0]/(double)imageSize.get(level)[0],
                getPixelSizeY()*(double)imageSize.get(0)[1]/(double)imageSize.get(level)[1],
                getPixelSizeZ()*(double)imageSize.get(0)[2]/(double)imageSize.get(level)[2]);
        transform.translate(new double[]{getStagePosX(),getStagePosY(),0});
        return transform;
    }

    public NumericType getNumericType(int channel) throws Exception {
        PixelsData pixels = getPixelsDataFromOmeroID(omeroImageID, gateway, securityContext);
        OmeroSource source;
        //TODO : get pixel type as a field in omerosourceopener
        switch(pixels.getPixelType()){
            case FLOAT_TYPE:
                return new FloatType();
            case UINT16_TYPE:
                return new UnsignedShortType();
            case UINT8_TYPE:
                return new UnsignedByteType();
            case UINT32_TYPE:
                return new UnsignedIntType();
            default:
                throw new IllegalStateException("Unsupported pixel type : " + pixels.getPixelType());
        }
    }

    public String getImageName() {
        return (this.imageName + "--OMERO ID:" + this.omeroImageID);
    }

    public static OmeroSourceOpener getOpener() {
        OmeroSourceOpener opener = new OmeroSourceOpener()
                .positionReferenceFrameLength(new ome.units.quantity.Length(1, UNITS.MICROMETER)) // Compulsory
                .voxSizeReferenceFrameLength(new ome.units.quantity.Length(1, UNITS.MICROMETER))
                .millimeter()
                .useCacheBlockSizeFromOmero(true);
        return opener;
    }

    public Dimensions getDimensions() {
        // Always set 3d to allow for Big Stitcher compatibility
        //int numDimensions = 2 + (omeMeta.getPixelsSizeZ(iSerie).getNumberValue().intValue()>1?1:0);
        int numDimensions = 3;

        int sX = imageSize.get(0)[0];
        int sY = imageSize.get(0)[1];
        int sZ = imageSize.get(0)[2];

        long[] dims = new long[3];

        dims[0] = sX;
        dims[1] = sY;
        dims[2] = sZ;

        Dimensions dimensions = new Dimensions() {
            @Override
            public void dimensions(long[] dimensions) {
                dimensions[0] = dims[0];
                dimensions[1] = dims[1];
                dimensions[2] = dims[2];
            }

            @Override
            public long dimension(int d) {
                return dims[d];
            }

            @Override
            public int numDimensions() {
                return numDimensions;
            }
        };

        return dimensions;
    }
    public VoxelDimensions getVoxelDimensions(){
        // Always 3 to allow for big stitcher compatibility
        int numDimensions = 3;

        double[] d = new double[3];
        d[0] = psizeX;
        d[1] = psizeY;
        d[2] = psizeZ;

        VoxelDimensions voxelDimensions;

        {
            assert numDimensions == 3;
            voxelDimensions = new VoxelDimensions() {

                double[] dims = {
                        d[0],
                        d[1],
                        d[2]};

                @Override
                public String unit() { return u.toString(); }

                @Override
                public void dimensions(double[] doubles) {
                    doubles[0] = dims[0];
                    doubles[1] = dims[1];
                    doubles[2] = dims[2];
                }

                @Override
                public double dimension(int i) {
                    return dims[i];
                }

                @Override
                public int numDimensions() {
                    return numDimensions;
                }
            };
        }
        return voxelDimensions;

    }

    public OmeroSourceOpener ignoreMetadata() {
        this.positionIgnoreBioFormatsMetaData = true;
        this.voxSizeIgnoreBioFormatsMetaData = true;
        return this;
    }

    public OmeroSourceOpener setCache(SharedQueue cc) {
        this.cc = cc;
        return this;
    }


}
