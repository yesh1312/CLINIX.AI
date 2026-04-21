/* Port of src/main/resources/static/js/app.js with Premium Features */
const state = {
    apiKey: 'AIzaSyBDdQmr9uRtDHuBVMfFuTEQe3kHPQx4b20', // Embedded key for standalone ease
    sessionId: null,
    isProcessing: false,
    currentFile: null,
    analysisCnt: 0,
    history: []
};

const SYSTEM_PROMPT = `You are NEURO-AI, an advanced Brain MRI Analysis Engine. 
STRICT ZERO-HALLUCINATION POLICY. If image quality is poor, say so.
ANALYSIS PROTOCOL:
1. QUALITY ASSESSMENT
2. ANOMALY DETECTION
3. TUMOR CHARACTERIZATION (if detected)
4. DIFFERENTIAL DIAGNOSIS
5. CLINICAL RECOMMENDATION

Respond ONLY with valid JSON:
{
  "imageQuality": "Good|Fair|Poor",
  "tumorDetected": true|false,
  "confidenceScore": 0-100,
  "tumorFindings": { "location": "string", "size": "string" },
  "likelyDiagnosis": "string",
  "regionHighlight": { "approximateX": 0-100, "approximateY": 0-100, "description": "string" },
  "keyMRIFeatures": ["string"],
  "clinicalRecommendation": "string",
  "urgencyLevel": "Routine|Urgent|Emergency"
}
STRICT SPATIAL ACCURACY: Ensure regionHighlight coordinates (approximateX, approximateY) correspond precisely to the lesion's center point (0-100 scale).`;

document.addEventListener('DOMContentLoaded', () => {
    initUI();
    console.log("CLINIX.AI Core Initialized");
});

function initUI() {
    const fileInput = document.getElementById('fileInput');
    const uploadBtn = document.getElementById('uploadBtn');
    const sendBtn = document.getElementById('sendBtn');
    const chatInput = document.getElementById('chatInput');

    if (uploadBtn) uploadBtn.onclick = () => fileInput.click();
    if (fileInput) fileInput.onchange = handleFileSelect;
    if (sendBtn) sendBtn.onclick = handleSend;
    if (chatInput) chatInput.onkeydown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };
}

function handleFileSelect(e) {
    const file = e.target.files[0];
    if (!file) return;
    state.currentFile = file;

    const reader = new FileReader();
    reader.onload = (ev) => {
        const thumb = document.getElementById('previewThumb');
        const preview = document.getElementById('uploadPreview');
        const welcome = document.getElementById('welcomeScreen');
        if (thumb) thumb.src = ev.target.result;
        if (preview) preview.style.display = 'block';
        if (welcome) welcome.style.display = 'none';
    };
    reader.readAsDataURL(file);
}

function clearUpload() {
    state.currentFile = null;
    const input = document.getElementById('fileInput');
    const preview = document.getElementById('uploadPreview');
    if (input) input.value = '';
    if (preview) preview.style.display = 'none';

    // Show welcome if no messages
    if (state.analysisCnt === 0) {
        document.getElementById('welcomeScreen').style.display = 'flex';
    }
}

async function handleSend() {
    if (state.isProcessing) return;
    const input = document.getElementById('chatInput');
    const text = input ? input.value.trim() : '';
    if (!text && !state.currentFile) return;

    setProcessing(true);
    const context = text;
    const file = state.currentFile;

    addUserMessage(text, file);
    if (input) input.value = '';
    clearUpload();

    const typingId = showTyping();

    try {
        let result;
        if (file) {
            const base64 = await fileToBase64(file);
            result = await callClinixVision(base64, file.type, context);
            renderAnalysisReport(result, base64, file.type);
            updateStats(result);
        } else {
            result = await callClinixText(context);
            addAITextMessage(result);
        }
    } catch (err) {
        addErrorMessage('Analysis failed: ' + err.message);
    } finally {
        removeTyping(typingId);
        setProcessing(false);
    }
}

async function simulateDemoAnalysis() {
    if (state.isProcessing) return;
    setProcessing(true);
    document.getElementById('welcomeScreen').style.display = 'none';

    addUserMessage("Run high-priority neural scan on demo MRI sample.", null);

    const typingId = showTyping();

    try {
        // Fetch the local demo asset
        const response = await fetch('assets/demo_scan.png');
        const blob = await response.blob();
        const base64 = await fileToBase64(blob);

        // Use flash model for speed
        const result = await callClinixVision(base64, 'image/png', "Analyze this demo scan.");

        renderAnalysisReport(result, base64, 'image/png');
        updateStats(result);

    } catch (err) {
        addErrorMessage('Demo Analysis failed: Make sure assets/demo_scan.png exists.');
    } finally {
        removeTyping(typingId);
        setProcessing(false);
    }
}

function updateStats(result) {
    state.analysisCnt++;
    const cntEl = document.getElementById('analysisCnt');
    const confEl = document.getElementById('lastConf');
    if (cntEl) cntEl.textContent = state.analysisCnt;
    if (confEl) confEl.textContent = (result.confidenceScore || 0) + '%';

    if (!state.sessionId) {
        state.sessionId = 'SN-' + Math.random().toString(36).substr(2, 9).toUpperCase();
        document.getElementById('sessionIdDisplay').textContent = state.sessionId;
    }
}

async function callClinixVision(base64, mimeType, prompt) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${state.apiKey}`;
    const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            contents: [{
                parts: [
                    { text: SYSTEM_PROMPT + "\n\nUser Question: " + (prompt || "Analyze this scan.") },
                    { inlineData: { mimeType, data: base64 } }
                ]
            }]
        })
    });

    if (!response.ok) throw new Error("API Connection Error");
    const data = await response.json();
    const raw = data.candidates[0].content.parts[0].text;
    return JSON.parse(raw.replace(/```json\n?|```/g, ''));
}

async function callClinixText(prompt) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${state.apiKey}`;
    const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            contents: [{ parts: [{ text: "You are NEURO-AI medical assistant. Answer concisely and professionally: " + prompt }] }]
        })
    });
    const data = await response.json();
    return data.candidates[0].content.parts[0].text;
}

function addUserMessage(text, file) {
    const container = document.getElementById('chatMessages');
    if (!container) return;
    const msg = document.createElement('div');
    msg.className = 'message user';

    let content = '';
    if (file) {
        const url = URL.createObjectURL(file);
        content += `<div class="uploaded-img-preview"><img src="${url}"></div>`;
    }
    content += `<div>${text || 'Initiating Analysis Protocol...'}</div>`;

    msg.innerHTML = `
        <div class="avatar user-av">👤</div>
        <div class="bubble">
            <div class="bubble-sender">PHYSICIAN</div>
            <div class="bubble-content user-bubble">${content}</div>
        </div>
    `;
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;
}

function showTyping() {
    const id = 'typing_' + Date.now();
    const container = document.getElementById('chatMessages');
    if (!container) return id;
    const msg = document.createElement('div');
    msg.id = id;
    msg.className = 'message';
    msg.innerHTML = `
        <div class="avatar ai">🧠</div>
        <div class="bubble">
            <div class="bubble-sender">NEURO-AI</div>
            <div class="typing-indicator">
                <span>Analyzing neural parameters</span>
                <div class="typing-dots">
                    <span></span><span></span><span></span>
                </div>
            </div>
        </div>
    `;
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;
    return id;
}

function removeTyping(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
}

function renderAnalysisReport(data, base64, mimeType) {
    const container = document.getElementById('chatMessages');
    if (!container) return;
    const canvId = 'canv_' + Date.now();
    const msg = document.createElement('div');
    msg.className = 'message';

    const urgencyClass = (data.urgencyLevel || 'Routine').toLowerCase();
    const urgencyHTML = `<span class="urgency-tag urgency-${urgencyClass}">${data.urgencyLevel || 'Routine'}</span>`;

    msg.innerHTML = `
        <div class="avatar ai">🧠</div>
        <div class="bubble" style="width: 100%;">
            <div class="bubble-sender">NEURO-AI REPORT</div>
            <div class="report-card">
                <div class="report-header ${data.tumorDetected ? 'tumor-detected' : 'no-tumor'}">
                    <div class="report-title">BRAIN MRI ANALYSIS</div>
                    <div class="detection-badge ${data.tumorDetected ? 'badge-positive' : 'badge-negative'}">
                        ${data.tumorDetected ? '⚠️ ANOMALY DETECTED' : '✓ CLEAR SCAN'}
                    </div>
                </div>
                <div class="confidence-meter">
                    <div style="display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:8px;">
                        <span style="font-family:var(--font-mono); font-size:10px; color:var(--text-muted); letter-spacing:1px;">CONFIDENCE SCORE</span>
                        <span class="meter-value" style="color:${data.tumorDetected ? 'var(--accent-red)' : 'var(--accent-teal)'}">${(data.confidenceScore || 0)}%</span>
                    </div>
                    <div class="meter-track">
                        <div class="meter-fill high" style="width: ${data.confidenceScore}%; background: ${data.tumorDetected ? 'linear-gradient(90deg, #ff3b5c, #ff8c42)' : 'linear-gradient(90deg, #00e676, #00d4b4)'}"></div>
                    </div>
                </div>
                <div style="padding: 20px; border-bottom: 1px solid var(--border); text-align:center; background: rgba(0,0,0,0.2);">
                    <div class="mri-canvas-container">
                        <img id="img_${canvId}" src="data:${mimeType};base64,${base64}" style="max-height:280px;">
                        <canvas id="${canvId}"></canvas>
                        <div class="scan-indicator">NEURAL_MAP_V1.5</div>
                    </div>
                </div>
                <div class="report-grid">
                    <div class="report-section">
                        <div class="rs-label">LIKELY DIAGNOSIS</div>
                        <div class="rs-value highlight">${data.likelyDiagnosis || 'No significant findings'}</div>
                    </div>
                    <div class="report-section">
                        <div class="rs-label">URGENCY LEVEL</div>
                        <div class="rs-value">${urgencyHTML}</div>
                    </div>
                    <div class="report-section full-width">
                        <div class="rs-label">FINDINGS & RECOMMENDATION</div>
                        <div class="rs-value">${data.clinicalRecommendation || 'Follow standard protocol.'}</div>
                    </div>
                </div>
            </div>
        </div>
    `;
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;

    setTimeout(() => drawOverlay(canvId, data.regionHighlight), 150);
}

function drawOverlay(id, rh) {
    const img = document.getElementById('img_' + id);
    const canv = document.getElementById(id);
    if (!img || !canv || !rh) return;

    canv.width = img.clientWidth;
    canv.height = img.clientHeight;
    const ctx = canv.getContext('2d');

    const x = (rh.approximateX / 100) * canv.width;
    const y = (rh.approximateY / 100) * canv.height;

    // Pulse effect simulation
    ctx.strokeStyle = '#ff3b5c';
    ctx.lineWidth = 3;
    ctx.setLineDash([5, 3]);
    ctx.beginPath();
    ctx.arc(x, y, 30, 0, Math.PI * 2);
    ctx.stroke();

    ctx.fillStyle = 'rgba(255, 59, 92, 0.25)';
    ctx.fill();

    ctx.fillStyle = '#ff3b5c';
    ctx.font = 'bold 11px Space Mono';
    ctx.shadowBlur = 4;
    ctx.shadowColor = 'black';
    ctx.fillText('ANOMALY_ZONE', x + 35, y);
}

function addAITextMessage(text) {
    const container = document.getElementById('chatMessages');
    if (!container) return;
    const msg = document.createElement('div');
    msg.className = 'message';
    msg.innerHTML = `
        <div class="avatar ai">🧠</div>
        <div class="bubble">
            <div class="bubble-sender">NEURO-AI</div>
            <div class="bubble-content ai-bubble">${text}</div>
        </div>
    `;
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;
}

function addErrorMessage(text) {
    addAITextMessage('<span style="color:var(--accent-red); font-family:var(--font-mono); font-size:12px;">' + text + '</span>');
}

function setProcessing(val) {
    state.isProcessing = val;
    const btn = document.getElementById('sendBtn');
    if (btn) btn.style.opacity = val ? '0.3' : '1';
}

function fileToBase64(file) {
    return new Promise((r, j) => {
        const reader = new FileReader();
        reader.onload = () => r(reader.result.split(',')[1]);
        reader.onerror = j;
        reader.readAsDataURL(file);
    });
}
