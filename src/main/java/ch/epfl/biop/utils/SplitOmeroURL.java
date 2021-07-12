package ch.epfl.biop.utils;

import org.scijava.plugin.Parameter;

public class SplitOmeroURL {
    /*
    Dataset:

    https://omero-poc.epfl.ch/webclient/?show=dataset-1111
    Image:
    https://omero-poc.epfl.ch/webclient/img_detail/3713/
    or
    https://omero-poc.epfl.ch/webclient/?show=image-3713

     */

    public String host;
    public long imageID;
}
