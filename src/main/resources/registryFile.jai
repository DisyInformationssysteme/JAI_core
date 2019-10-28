#
# $RCSfile: registryFile.jai,v $
#
# Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
#
# Use is subject to license terms.
#
# $Revision: 1.2 $
# $Date: 2005-08-31 22:35:25 $
# $State: Exp $
#
# The master JAI registry initialization file
#
############################################################################
#
# Each line can be in one of the formats described below. Space or tab
# characters separate keywords in each line. The comment character is
# '#'; on each line all characters following the first comment character
# are ignored. The file must be encoded in UTF-8.
#
# 1. To register descriptors :
#
#	descriptor <descriptor-class-name>
#	odesc	   <descriptor-class-name>  <descriptor-name>
#
#    The second version above is deprecated and is retained for backward
#    compatibility with JAI 1.0.2. Descriptors are always registered
#    against <descriptor-class>.getName(). The <descriptor-name> in the
#    second version is always ignored.
#
# 2. To register factory objects under a product against a specific mode :
#
#	<registry-mode-name>	<factory-class-name>	<product-name>	<descriptor-name>   <local-name>
#	<registry-mode-name>	<factory-class-name>	<descriptor-name>
#
#    The first version above is used to register factory objects against
#    modes that support preferences. The second version is used for those
#    that do not support preferences. <local-name> is an arbitrary name that
#    is unique for a given mode. This is (only) used later on in this file
#    to set preferences between factory objects.
#
# 3. To set preferences between products for a descriptor under a
#    specific mode :
#
#	prefProduct <modeName>	<descriptor-name>   <preferred-product-name> <other-product-name>
#	pref	    product	<descriptor-name>   <preferred-product-name> <other-product-name>
#
#    The second version above is deprecated and is retained for backward
#    compatibility with JAI 1.0.2. This version is assumed to set
#    product preferences for the "rendered" mode.
#
# 4. To set preferences between factory objects for descriptor under a
#    a specific product and registry mode :
#
#	pref	<modeName>  <descriptor-name>	<product-name>	<preferred-factory-local-name>	<other-factory-local-name>
#
############################################################################
#
# Image operation descriptors :
#
descriptor	javax.media.jai.operator.AbsoluteDescriptor
descriptor	javax.media.jai.operator.AddCollectionDescriptor
descriptor	javax.media.jai.operator.AddConstDescriptor
descriptor	javax.media.jai.operator.AddConstToCollectionDescriptor
descriptor	javax.media.jai.operator.AddDescriptor
descriptor	javax.media.jai.operator.AffineDescriptor
descriptor	javax.media.jai.operator.AndConstDescriptor
descriptor	javax.media.jai.operator.AndDescriptor
descriptor	javax.media.jai.operator.AWTImageDescriptor
descriptor	javax.media.jai.operator.BandCombineDescriptor
descriptor	javax.media.jai.operator.BandMergeDescriptor
descriptor	javax.media.jai.operator.BandSelectDescriptor
descriptor	javax.media.jai.operator.BinarizeDescriptor
descriptor	javax.media.jai.operator.BMPDescriptor
descriptor	javax.media.jai.operator.BorderDescriptor
descriptor	javax.media.jai.operator.BoxFilterDescriptor
descriptor	javax.media.jai.operator.ClampDescriptor
descriptor	javax.media.jai.operator.ColorConvertDescriptor
descriptor	javax.media.jai.operator.ColorQuantizerDescriptor
descriptor	javax.media.jai.operator.ConstantDescriptor
descriptor	javax.media.jai.operator.CompositeDescriptor
descriptor	javax.media.jai.operator.ConjugateDescriptor
descriptor	javax.media.jai.operator.ConvolveDescriptor
descriptor	javax.media.jai.operator.CropDescriptor
descriptor	javax.media.jai.operator.DCTDescriptor
descriptor	javax.media.jai.operator.DFTDescriptor
descriptor	javax.media.jai.operator.DilateDescriptor
descriptor	javax.media.jai.operator.DivideDescriptor
descriptor	javax.media.jai.operator.DivideComplexDescriptor
descriptor	javax.media.jai.operator.DivideByConstDescriptor
descriptor	javax.media.jai.operator.DivideIntoConstDescriptor
descriptor	javax.media.jai.operator.ErodeDescriptor
descriptor	javax.media.jai.operator.ErrorDiffusionDescriptor
descriptor	javax.media.jai.operator.EncodeDescriptor
descriptor	javax.media.jai.operator.ExpDescriptor
descriptor	javax.media.jai.operator.ExtremaDescriptor
descriptor	javax.media.jai.operator.FileLoadDescriptor
descriptor	javax.media.jai.operator.FileStoreDescriptor
descriptor	javax.media.jai.operator.FilteredSubsampleDescriptor
descriptor	javax.media.jai.operator.FormatDescriptor
descriptor	javax.media.jai.operator.FPXDescriptor
descriptor	javax.media.jai.operator.GIFDescriptor
descriptor	javax.media.jai.operator.GradientMagnitudeDescriptor
descriptor	javax.media.jai.operator.HistogramDescriptor
descriptor	javax.media.jai.operator.IDCTDescriptor
descriptor	javax.media.jai.operator.IDFTDescriptor
descriptor	javax.media.jai.operator.IIPDescriptor
descriptor	javax.media.jai.operator.IIPResolutionDescriptor
descriptor	javax.media.jai.operator.ImageFunctionDescriptor
descriptor	javax.media.jai.operator.InvertDescriptor
descriptor	javax.media.jai.operator.JPEGDescriptor
descriptor	javax.media.jai.operator.LogDescriptor
descriptor	javax.media.jai.operator.LookupDescriptor
descriptor	javax.media.jai.operator.MagnitudeDescriptor
descriptor	javax.media.jai.operator.MagnitudeSquaredDescriptor
descriptor	javax.media.jai.operator.MaxDescriptor
descriptor	javax.media.jai.operator.MaxFilterDescriptor
descriptor	javax.media.jai.operator.MatchCDFDescriptor
descriptor	javax.media.jai.operator.MeanDescriptor
descriptor	javax.media.jai.operator.MedianFilterDescriptor
descriptor	javax.media.jai.operator.MinDescriptor
descriptor	javax.media.jai.operator.MinFilterDescriptor
descriptor	javax.media.jai.operator.MosaicDescriptor
descriptor	javax.media.jai.operator.MultiplyConstDescriptor
descriptor	javax.media.jai.operator.MultiplyComplexDescriptor
descriptor	javax.media.jai.operator.MultiplyDescriptor
descriptor	javax.media.jai.operator.NotDescriptor
descriptor	javax.media.jai.operator.NullDescriptor
descriptor	javax.media.jai.operator.OrConstDescriptor
descriptor	javax.media.jai.operator.OrDescriptor
descriptor	javax.media.jai.operator.OrderedDitherDescriptor
descriptor	javax.media.jai.operator.OverlayDescriptor
descriptor	javax.media.jai.operator.PatternDescriptor
descriptor	javax.media.jai.operator.PeriodicShiftDescriptor
descriptor	javax.media.jai.operator.PhaseDescriptor
descriptor	javax.media.jai.operator.PiecewiseDescriptor
descriptor	javax.media.jai.operator.PNGDescriptor
descriptor	javax.media.jai.operator.PNMDescriptor
descriptor	javax.media.jai.operator.PolarToComplexDescriptor
descriptor	javax.media.jai.operator.RenderableDescriptor
descriptor	javax.media.jai.operator.RescaleDescriptor
descriptor	javax.media.jai.operator.RotateDescriptor
descriptor	javax.media.jai.operator.ScaleDescriptor
descriptor	javax.media.jai.operator.ShearDescriptor
descriptor	javax.media.jai.operator.StreamDescriptor
descriptor	javax.media.jai.operator.SubsampleAverageDescriptor
descriptor	javax.media.jai.operator.SubsampleBinaryToGrayDescriptor
descriptor	javax.media.jai.operator.SubtractDescriptor
descriptor	javax.media.jai.operator.SubtractConstDescriptor
descriptor	javax.media.jai.operator.SubtractFromConstDescriptor
descriptor	javax.media.jai.operator.TIFFDescriptor
descriptor	javax.media.jai.operator.ThresholdDescriptor
descriptor	javax.media.jai.operator.TranslateDescriptor
descriptor	javax.media.jai.operator.TransposeDescriptor
descriptor	javax.media.jai.operator.UnsharpMaskDescriptor
descriptor	javax.media.jai.operator.URLDescriptor
descriptor	javax.media.jai.operator.WarpDescriptor
descriptor	javax.media.jai.operator.XorConstDescriptor
descriptor	javax.media.jai.operator.XorDescriptor

#
# tile codec descriptor
#
descriptor	javax.media.jai.tilecodec.GZIPTileCodecDescriptor
descriptor	javax.media.jai.tilecodec.RawTileCodecDescriptor

#
# remote descriptor
#
descriptor    javax.media.jai.remote.JAIRMIDescriptor

#
# "rendered" factory objects
#
rendered    com.sun.media.jai.opimage.AbsoluteCRIF		com.sun.media.jai	absolute		sunabsoluterif
rendered    com.sun.media.jai.opimage.AddCollectionCRIF		com.sun.media.jai	addcollection		sunaddcollectionrif
rendered    com.sun.media.jai.opimage.AddConstCRIF		com.sun.media.jai	addconst		sunaddconstrif
rendered    com.sun.media.jai.opimage.AddCRIF			com.sun.media.jai	add			sunaddrif
rendered    com.sun.media.jai.opimage.AffineCRIF		com.sun.media.jai	affine			sunaffinerif
rendered    com.sun.media.jai.opimage.AndConstCRIF		com.sun.media.jai	andconst		sunandconstrif
rendered    com.sun.media.jai.opimage.AndCRIF			com.sun.media.jai	and			sunandrif
rendered    com.sun.media.jai.opimage.AWTImageRIF		com.sun.media.jai	awtimage		sunawtimagerif
rendered    com.sun.media.jai.opimage.BandCombineCRIF		com.sun.media.jai	bandcombine		sunbandcombinerif
rendered    com.sun.media.jai.opimage.BandMergeCRIF		com.sun.media.jai	bandmerge		sunbandmergerif
rendered    com.sun.media.jai.opimage.BandSelectCRIF		com.sun.media.jai	bandselect		sunbandselectrif
rendered    com.sun.media.jai.opimage.BinarizeCRIF		com.sun.media.jai	binarize		sunbinarizerif
rendered    com.sun.media.jai.opimage.BMPRIF			com.sun.media.jai	bmp			sunbmprif
rendered    com.sun.media.jai.opimage.BorderRIF			com.sun.media.jai	border			sunborderrif
rendered    com.sun.media.jai.opimage.BoxFilterRIF		com.sun.media.jai	boxfilter		sunboxfilterrif
rendered    com.sun.media.jai.opimage.ClampCRIF			com.sun.media.jai	clamp			sunclamprif
rendered    com.sun.media.jai.opimage.ColorConvertCRIF		com.sun.media.jai	colorconvert		suncolorconvertrif
rendered    com.sun.media.jai.opimage.ColorQuantizerRIF		com.sun.media.jai	colorquantizer		suncolorquantizerrif
rendered    com.sun.media.jai.opimage.CompositeCRIF		com.sun.media.jai	composite		suncompositerif
rendered    com.sun.media.jai.opimage.ConstantCRIF		com.sun.media.jai	constant		sunconstrif1
rendered    com.sun.media.jai.opimage.ConjugateCRIF		com.sun.media.jai	conjugate		sunconjugaterif
rendered    com.sun.media.jai.opimage.ConvolveRIF		com.sun.media.jai	convolve		sunconvolverif
rendered    com.sun.media.jai.opimage.CropCRIF			com.sun.media.jai	crop			suncroprif
rendered    com.sun.media.jai.opimage.DCTCRIF			com.sun.media.jai	dct			sundctrif
rendered    com.sun.media.jai.opimage.DFTCRIF			com.sun.media.jai	dft			sundftrif
rendered    com.sun.media.jai.opimage.DilateRIF			com.sun.media.jai	dilate			sundilaterif
rendered    com.sun.media.jai.opimage.DivideCRIF		com.sun.media.jai	divide			sundividerif
rendered    com.sun.media.jai.opimage.DivideComplexCRIF		com.sun.media.jai	dividecomplex		sundividecomplexrif
rendered    com.sun.media.jai.opimage.DivideByConstCRIF		com.sun.media.jai	dividebyconst		sundividebyconstrif
rendered    com.sun.media.jai.opimage.DivideIntoConstCRIF	com.sun.media.jai	divideintoconst		sundivideintoconstrif
rendered    com.sun.media.jai.opimage.EncodeRIF			com.sun.media.jai	encode   		sunencoderif
rendered    com.sun.media.jai.opimage.ErodeRIF			com.sun.media.jai	erode			suneroderif
rendered    com.sun.media.jai.opimage.ErrorDiffusionRIF		com.sun.media.jai	errordiffusion		sunerrordiffusionrif
rendered    com.sun.media.jai.opimage.ExpCRIF		        com.sun.media.jai	exp			sunexprif
rendered    com.sun.media.jai.opimage.ExtremaRIF		com.sun.media.jai	extrema			sunextremarif
rendered    com.sun.media.jai.opimage.FileLoadRIF		com.sun.media.jai	fileload		sunfileloadrif
rendered    com.sun.media.jai.opimage.FileStoreRIF		com.sun.media.jai	filestore		sunfilestorerif
rendered    com.sun.media.jai.opimage.FilteredSubsampleRIF      com.sun.media.jai       filteredsubsample       sunfilteredsubsamplerif
rendered    com.sun.media.jai.opimage.FormatCRIF		com.sun.media.jai	format			sunformatrif
rendered    com.sun.media.jai.opimage.FPXRIF		        com.sun.media.jai	fpx			sunfpxrif
rendered    com.sun.media.jai.opimage.GIFRIF		        com.sun.media.jai	gif			sungifrif
rendered    com.sun.media.jai.opimage.GradientRIF		com.sun.media.jai	gradientmagnitude	sungradientrif
rendered    com.sun.media.jai.opimage.HistogramRIF		com.sun.media.jai	histogram		sunhistogramrif
rendered    com.sun.media.jai.opimage.IDCTCRIF			com.sun.media.jai	idct			sunidctrif
rendered    com.sun.media.jai.opimage.IDFTCRIF			com.sun.media.jai	idft			sunidftrif
rendered    com.sun.media.jai.opimage.IIPCRIF			com.sun.media.jai	iip			suniipcrif
rendered    com.sun.media.jai.opimage.IIPResolutionRIF		com.sun.media.jai	iipresolution		suniipresolutionrif
rendered    com.sun.media.jai.opimage.ImageFunctionRIF		com.sun.media.jai	imagefunction		sunimagefunctionrif
rendered    com.sun.media.jai.opimage.InvertCRIF		com.sun.media.jai	invert			suninvertrif
rendered    com.sun.media.jai.opimage.JPEGRIF		        com.sun.media.jai	jpeg			sunjpegrif
rendered    com.sun.media.jai.opimage.LogCRIF		        com.sun.media.jai	log			sunlogrif
rendered    com.sun.media.jai.opimage.LookupCRIF		com.sun.media.jai	lookup			sunlookuprif
rendered    com.sun.media.jai.opimage.MagnitudeCRIF		com.sun.media.jai	magnitude		sunmagnituderif
rendered    com.sun.media.jai.opimage.MagnitudeSquaredCRIF	com.sun.media.jai	magnitudesquared	sunmagnitudesquaredrif
rendered    com.sun.media.jai.opimage.MatchCDFCRIF		com.sun.media.jai	matchcdf		sunmatchcdfrif
rendered    com.sun.media.jai.opimage.MaxCRIF		        com.sun.media.jai	max			sunmaxrif
rendered    com.sun.media.jai.opimage.MaxFilterRIF		com.sun.media.jai	maxfilter		sunmaxfilterrif
rendered    com.sun.media.jai.opimage.MeanRIF		        com.sun.media.jai	mean			sunmeanrif
rendered    com.sun.media.jai.opimage.MedianFilterRIF		com.sun.media.jai	medianfilter		sunmedianfilterrif
rendered    com.sun.media.jai.opimage.MinCRIF		        com.sun.media.jai	min			sunminrif
rendered    com.sun.media.jai.opimage.MinFilterRIF		com.sun.media.jai	minfilter		sunminfilterrif
rendered    com.sun.media.jai.opimage.MosaicRIF		com.sun.media.jai	mosaic		sunmosaicrif
rendered    com.sun.media.jai.opimage.MultiplyCRIF		com.sun.media.jai	multiply		sunmultiplyrif
rendered    com.sun.media.jai.opimage.MultiplyComplexCRIF	com.sun.media.jai	multiplycomplex		sunmultiplycomplexrif
rendered    com.sun.media.jai.opimage.MultiplyConstCRIF		com.sun.media.jai	multiplyconst		sunmultiplyconstrif
rendered    com.sun.media.jai.opimage.NotCRIF			com.sun.media.jai	not			sunnotrif
rendered    javax.media.jai.NullCRIF			   	javax.media.jai		null			sunullrif
rendered    com.sun.media.jai.opimage.OrConstCRIF		com.sun.media.jai	orconst			sunorconstrif
rendered    com.sun.media.jai.opimage.OrCRIF			com.sun.media.jai	or			sunorrif
rendered    com.sun.media.jai.opimage.OrderedDitherRIF		com.sun.media.jai	ordereddither		sunorderedditherrif
rendered    com.sun.media.jai.opimage.OverlayCRIF		com.sun.media.jai	overlay			sunoverlayrif
rendered    com.sun.media.jai.opimage.PatternRIF		com.sun.media.jai	pattern			sunpatternrif
rendered    com.sun.media.jai.opimage.PhaseCRIF			com.sun.media.jai	phase			sunphaserif
rendered    com.sun.media.jai.opimage.PeriodicShiftCRIF		com.sun.media.jai	periodicshift		sunperiodicshiftrif
rendered    com.sun.media.jai.opimage.PiecewiseCRIF		com.sun.media.jai	piecewise		sunpiecewiserif
rendered    com.sun.media.jai.opimage.PNGRIF			com.sun.media.jai	png			sunpngrif
rendered    com.sun.media.jai.opimage.PNMRIF			com.sun.media.jai	pnm			sunpnmrif
rendered    com.sun.media.jai.opimage.PolarToComplexCRIF	com.sun.media.jai	polartocomplex		sunpolartocomplexrif
rendered    com.sun.media.jai.opimage.RescaleCRIF		com.sun.media.jai	rescale			sunrescalerif
rendered    com.sun.media.jai.opimage.RotateCRIF		com.sun.media.jai	rotate			sunrotaterif
rendered    com.sun.media.jai.opimage.ScaleCRIF			com.sun.media.jai	scale			sunscalerif
rendered    com.sun.media.jai.opimage.ShearRIF			com.sun.media.jai	shear			sunshearrif
rendered    com.sun.media.jai.opimage.StreamRIF			com.sun.media.jai	stream			sunstreamrif
rendered    com.sun.media.jai.opimage.SubsampleBinaryToGrayCRIF	com.sun.media.jai	subsamplebinarytogray	sunsubsamplebinarytograyrif
rendered    com.sun.media.jai.opimage.SubsampleAverageCRIF	com.sun.media.jai	subsampleaverage	sunsubsampleaveragerif
rendered    com.sun.media.jai.opimage.SubtractCRIF		com.sun.media.jai	subtract		sunsubtractrif
rendered    com.sun.media.jai.opimage.SubtractConstCRIF		com.sun.media.jai	subtractconst		sunsubtractconstrif
rendered    com.sun.media.jai.opimage.SubtractFromConstCRIF	com.sun.media.jai	subtractfromconst	sunsubtractfromconstrif
rendered    com.sun.media.jai.opimage.TIFFRIF			com.sun.media.jai	tiff			suntiffrif
rendered    com.sun.media.jai.opimage.ThresholdCRIF		com.sun.media.jai	threshold		sunthresholdrif
rendered    com.sun.media.jai.opimage.TranslateCRIF		com.sun.media.jai	translate		suntransrif
rendered    com.sun.media.jai.opimage.TransposeCRIF		com.sun.media.jai	transpose		suntransposerif
rendered    com.sun.media.jai.opimage.UnsharpMaskRIF		com.sun.media.jai	unsharpmask		sununsharpmaskrif
rendered    com.sun.media.jai.opimage.URLRIF			com.sun.media.jai	url			sunurlrif
rendered    com.sun.media.jai.opimage.WarpRIF			com.sun.media.jai	warp			sunwarprif
rendered    com.sun.media.jai.opimage.XorConstCRIF		com.sun.media.jai	xorconst		sunxorconstrif
rendered    com.sun.media.jai.opimage.XorCRIF			com.sun.media.jai	xor			sunxorrif


#
# "renderable" factory objects
#
renderable	com.sun.media.jai.opimage.AbsoluteCRIF		absolute
renderable	com.sun.media.jai.opimage.AddCollectionCRIF	addcollection
renderable	com.sun.media.jai.opimage.AddConstCRIF		addconst
renderable	com.sun.media.jai.opimage.AddCRIF		add
renderable	com.sun.media.jai.opimage.AffineCRIF		affine
renderable	com.sun.media.jai.opimage.AndConstCRIF		andconst
renderable	com.sun.media.jai.opimage.AndCRIF		and
renderable	com.sun.media.jai.opimage.BandCombineCRIF	bandcombine
renderable	com.sun.media.jai.opimage.BandMergeCRIF         bandmerge
renderable	com.sun.media.jai.opimage.BandSelectCRIF	bandselect
renderable	com.sun.media.jai.opimage.BinarizeCRIF		binarize
renderable	com.sun.media.jai.opimage.ClampCRIF		clamp
renderable	com.sun.media.jai.opimage.ColorConvertCRIF	colorconvert
renderable	com.sun.media.jai.opimage.CompositeCRIF		composite
renderable	com.sun.media.jai.opimage.ConjugateCRIF		conjugate
renderable	com.sun.media.jai.opimage.ConstantCRIF		constant
renderable	com.sun.media.jai.opimage.CropCRIF		crop
renderable	com.sun.media.jai.opimage.DCTCRIF		dct
renderable	com.sun.media.jai.opimage.DFTCRIF		dft
renderable	com.sun.media.jai.opimage.DivideCRIF		divide
renderable	com.sun.media.jai.opimage.DivideComplexCRIF	dividecomplex
renderable	com.sun.media.jai.opimage.DivideByConstCRIF	dividebyconst
renderable	com.sun.media.jai.opimage.DivideIntoConstCRIF	divideintoconst
renderable	com.sun.media.jai.opimage.ExpCRIF		exp
renderable	com.sun.media.jai.opimage.FormatCRIF		format
renderable	com.sun.media.jai.opimage.IDCTCRIF		idct
renderable	com.sun.media.jai.opimage.IDFTCRIF		idft
renderable	com.sun.media.jai.opimage.IIPCRIF		iip
renderable	com.sun.media.jai.opimage.InvertCRIF		invert
renderable	com.sun.media.jai.opimage.LogCRIF		log
renderable	com.sun.media.jai.opimage.LookupCRIF		lookup
renderable	com.sun.media.jai.opimage.MagnitudeCRIF		magnitude
renderable	com.sun.media.jai.opimage.MagnitudeSquaredCRIF	magnitudesquared
renderable	com.sun.media.jai.opimage.MatchCDFCRIF		matchcdf
renderable	com.sun.media.jai.opimage.MaxCRIF		max
renderable	com.sun.media.jai.opimage.MinCRIF		min
renderable	com.sun.media.jai.opimage.MultiplyCRIF		multiply
renderable	com.sun.media.jai.opimage.MultiplyComplexCRIF	multiplycomplex
renderable	com.sun.media.jai.opimage.MultiplyConstCRIF	multiplyconst
renderable	com.sun.media.jai.opimage.NotCRIF		not
renderable	javax.media.jai.NullCRIF			null
renderable	com.sun.media.jai.opimage.OrConstCRIF		orconst
renderable	com.sun.media.jai.opimage.OrCRIF		or
renderable	com.sun.media.jai.opimage.OverlayCRIF		overlay
renderable	com.sun.media.jai.opimage.PeriodicShiftCRIF	periodicshift
renderable	com.sun.media.jai.opimage.PhaseCRIF		phase
renderable	com.sun.media.jai.opimage.PiecewiseCRIF		piecewise
renderable	com.sun.media.jai.opimage.PolarToComplexCRIF	polartocomplex
renderable	com.sun.media.jai.opimage.RenderableCRIF	renderable
renderable	com.sun.media.jai.opimage.RescaleCRIF		rescale
renderable	com.sun.media.jai.opimage.RotateCRIF		rotate
renderable	com.sun.media.jai.opimage.ScaleCRIF		scale
renderable	com.sun.media.jai.opimage.SubsampleBinaryToGrayCRIF	subsamplebinarytogray
renderable	com.sun.media.jai.opimage.SubsampleAverageCRIF	subsampleaverage
renderable	com.sun.media.jai.opimage.SubtractCRIF		subtract
renderable	com.sun.media.jai.opimage.SubtractConstCRIF	subtractconst
renderable	com.sun.media.jai.opimage.SubtractFromConstCRIF	subtractfromconst
renderable	com.sun.media.jai.opimage.ThresholdCRIF		threshold
renderable	com.sun.media.jai.opimage.TranslateCRIF		translate
renderable	com.sun.media.jai.opimage.TransposeCRIF		transpose
renderable	com.sun.media.jai.opimage.XorConstCRIF		xorconst
renderable	com.sun.media.jai.opimage.XorCRIF		xor

#
# "collection" factory objects
#
collection	com.sun.media.jai.opimage.AddConstToCollectionCIF	com.sun.media.jai	addconsttocollection	sunaddconsttocollectioncif

#
# tile decoder factory objects
#
tileDecoder com.sun.media.jai.tilecodec.GZIPTileDecoderFactory	com.sun.media.jai	gzip			sungziptiledecoderfactory
tileDecoder com.sun.media.jai.tilecodec.RawTileDecoderFactory	com.sun.media.jai	raw			sunrawtiledecoderfactory

#
# tile encoder factory objects
#
tileEncoder com.sun.media.jai.tilecodec.GZIPTileEncoderFactory	com.sun.media.jai	gzip			sungziptileencoderfactory
tileEncoder com.sun.media.jai.tilecodec.RawTileEncoderFactory	com.sun.media.jai	raw			sunrawtileencoderfactory

#
# remote rendered factory objects
#
remoterendered      com.sun.media.jai.rmi.JAIRMICRIF             jairmi

#
# remote renderable factory objects
#
remoterenderable    com.sun.media.jai.rmi.JAIRMICRIF             jairmi

#
# "rendered" factory object preferences
#
