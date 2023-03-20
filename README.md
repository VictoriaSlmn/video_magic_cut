# Smart Scene

Summarise only saliency parts of video by ML (video create hackathon project).

## Pipeline
- Pick video from Gallery
- Decode each frame to Bitmap with timestamp
- Process each frame by TF-lite model (for first iteration will use open-source [detection model]( https://tfhub.dev/tensorflow/lite-model/efficientdet/lite2/detection/metadata/1))
- Accumulate data from detection model by frame timestamp
- Choose video segments based on the getting data and build in logic
- Generate video based on chhosed video segments and save in Gallery
