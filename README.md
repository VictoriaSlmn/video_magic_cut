# Smart Scene

Summarise only saliency parts of video by ML (video create hackathon project).

## Pipeline
- Pick video from Gallery
- Decode each frame to Bitmap with timestamp
- Process each frame by TF-lite model (for first iteration will use open-source [detection model]( https://tfhub.dev/tensorflow/lite-model/efficientdet/lite2/detection/metadata/1))
- Accumulate data from detection model by frame timestamp
- Choose video segments based on the getting data and build in logic (if segment longer than 1sec and contains interesting object like cat, person or dog)
- Generate video based on choosing video segments and save in Gallery (cutting of videos happen now by closest key-frames because for more precise video cutting we need decode/encode which it makes code more complex)

## Results

Processing image by TF-lite model pretty slow for video is around 70ms per frame and around 10ms we need for decode video frame and convert it to image (we can optimize it though). So if we have video which is has 30fps, 1 sec of processing those video wild take 2,5sec. What we can try - we can process only key-frames. More details from logging is [here](https://docs.google.com/spreadsheets/d/1xgMzG97rmLjzkCthkMZ6HDdGPA1HunOQdzvZZYaPBhE/edit?usp=sharing)

### Examples
| Input video | Output video |
| :-------- | :------- |
| https://drive.google.com/file/d/1Fm9rnLAsEK0-8qbb170u1C9pvwd3mSbl/view?usp=sharing | https://drive.google.com/file/d/19mrj-jhNYQNN1R7wazZHWum79R97nDzV/view?usp=sharing |
