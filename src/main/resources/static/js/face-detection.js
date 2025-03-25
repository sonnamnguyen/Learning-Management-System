const video = document.getElementById("video");
const loadingSpinner = document.getElementById("loadingSpinner");
const cameraStatus = document.getElementById("cameraStatus");
const videoContainer = document.getElementById("videoContainer");

// Disable all answer inputs and exercise links initially
function disableInteractions() {
    document.querySelectorAll(".form-check-input, .exercise-title a").forEach(element => {
        element.disabled = true;
        element.classList.add("disabled");
    })
}

// Function to enable interactions
function enableInteractions() {
    document.querySelectorAll(".form-check-input, .exercise-title a").forEach(element => {
        element.disabled = false;
        element.classList.remove("disabled");
    })
}

// Function to setup camera
async function setupCamera() {
    loadingSpinner.style.display = "block";
    try {
        let stream = await navigator.mediaDevices.getUserMedia({
            video: {
                width: { ideal: 640 },
                height: { ideal: 480 },
                facingMode: "user",
            },
        });
        video.srcObject = stream;

        // Wait for video metadata to load to ensure dimensions are available
        await new Promise(resolve => video.onloadedmetadata = resolve);

        // Set explicit dimensions on video element
        if (!video.width) video.width = 640
        if (!video.height) video.height = 480

        video.play();
        cameraStatus.textContent = "Camera Active";
        videoContainer.classList.add("video-active");
        enableInteractions();
    } catch (error) {
        console.error("Error accessing camera: ", error);
        alert("❌ You must allow camera access to proceed with the exam.");
        cameraStatus.textContent = "No camera";
        disableInteractions();
    } finally {
        loadingSpinner.style.display = "none";
    }
}

// Load face-api models
Promise.all([
    faceapi.nets.tinyFaceDetector.loadFromUri("/models"),
    faceapi.nets.faceLandmark68Net.loadFromUri("/models"),
    faceapi.nets.faceRecognitionNet.loadFromUri("/models"),
    faceapi.nets.faceExpressionNet.loadFromUri("/models"),
]).then(setupCamera);

// Face detection on client-side
video.addEventListener("play", function () {
    // Remove any existing canvas first to prevent duplicates
    const existingCanvas = document.querySelector(".face-detection-canvas")
    if (existingCanvas) {
        existingCanvas.remove()
    }

    // Create canvas and position it properly
    const canvas = faceapi.createCanvasFromMedia(video);
    canvas.className = "face-detection-canvas";

    // Style the canvas to overlay on top of the video
    canvas.style.position = "absolute"
    canvas.style.top = "0"
    canvas.style.left = "0"
    canvas.style.width = "100%"
    canvas.style.height = "100%"

    // Append canvas to the video container instead of body
    videoContainer.appendChild(canvas)

    // Get actual dimensions from the video element's computed style
    // This ensures we have valid dimensions even if not set directly on the video
    const videoWidth = video.clientWidth || video.offsetWidth || 320
    const videoHeight = video.clientHeight || video.offsetHeight || 240

    const displaySize = { width: videoWidth, height: videoHeight }
    faceapi.matchDimensions(canvas, displaySize);

    // Track face detection violations
    let noFaceDetectedCount = 0;
    let multipleFacesDetectedCount = 0;

    setInterval(async () => {
        // Update dimensions on each frame to ensure accuracy
        const currentWidth = video.clientWidth || video.offsetWidth || 320
        const currentHeight = video.clientHeight || video.offsetHeight || 240
        const currentDisplaySize = { width: currentWidth, height: currentHeight }

        // Only update dimensions if they've changed
        if (displaySize.width !== currentWidth || displaySize.height !== currentHeight) {
            faceapi.matchDimensions(canvas, currentDisplaySize)
        }

        const detections = await faceapi
            .detectAllFaces(video, new faceapi.TinyFaceDetectorOptions())
            .withFaceLandmarks()
            .withFaceExpressions();

        const resizedDetections = faceapi.resizeResults(detections, displaySize);
        canvas.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);

        // Draw detections
        faceapi.draw.drawDetections(canvas, resizedDetections);
        faceapi.draw.drawFaceLandmarks(canvas, resizedDetections);
        faceapi.draw.drawFaceExpressions(canvas, resizedDetections);

        // Check for violations (no face or multiple faces)
        if (detections.length === 0) {
            noFaceDetectedCount++;
            if (noFaceDetectedCount > 10) {
                // After ~1 second of no face
                cameraStatus.textContent = "⚠️ Face not detected";
                cameraStatus.style.backgroundColor = "rgba(255, 0, 0, 0.7)";

                // Update violation counter in session storage
                violationFaceCount++;
                sessionStorage.setItem("violationFaceCount", violationFaceCount.toString());
            }
        } else if (detections.length > 1) {
            multipleFacesDetectedCount++;
            if (multipleFacesDetectedCount > 10) {
                // After ~1 second of multiple faces
                cameraStatus.textContent = "⚠️ Multiple faces detected";
                cameraStatus.style.backgroundColor = "rgba(255, 0, 0, 0.7)";

                // Update violation counter in session storage
                violationFaceCount++;
                sessionStorage.setItem("violationFaceCount", violationFaceCount.toString());
            }
        } else {
            // Reset counters when face detection is normal
            noFaceDetectedCount = 0;
            multipleFacesDetectedCount = 0;
            cameraStatus.textContent = "Camera Active";
            cameraStatus.style.backgroundColor = "rgba(0, 0, 0, 0.7)";
        }
    }, 100);
})

// Make sure canvas resizes with video
window.addEventListener("resize", () => {
    const canvas = document.querySelector(".face-detection-canvas")
    if (canvas && video) {
        const displaySize = { width: video.clientWidth, height: video.clientHeight }
        faceapi.matchDimensions(canvas, displaySize)
    }
})

