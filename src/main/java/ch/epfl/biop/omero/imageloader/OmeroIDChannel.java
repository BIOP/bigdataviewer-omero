package ch.epfl.biop.omero.imageloader;

public class OmeroIDChannel {

    public final int OmeroID;
    public final int iChannel;

    public OmeroIDChannel(int imageID, int c) {
        OmeroID=imageID;
        iChannel=c;
    }

}
