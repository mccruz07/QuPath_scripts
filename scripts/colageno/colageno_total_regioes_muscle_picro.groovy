setImageType('BRIGHTFIELD_OTHER')
setColorDeconvolutionStains('{"Name" : "H-DAB default", "Stain 1" : "Hematoxylin", "Values 1" : "0.6511078257574492 0.7011930431234068 0.29049426072255424", "Stain 2" : "DAB", "Values 2" : "0.26916687204956063 0.5682411743268502 0.777593185920953", "Background" : " 255 255 255"}');
createAnnotationsFromPixelClassifier("muscle_picro", 1500.0, 0.0)
selectObjects(getAnnotationObjects().findAll {it.getPathClass() == getPathClass("Negative")})
createAnnotationsFromPixelClassifier("collagen_total", 3.0, 0.0)
