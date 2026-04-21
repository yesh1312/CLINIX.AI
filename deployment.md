# Deployment Guide: Sharing CLINIX.AI

To "make this project as a link" for your conference paper, follow these steps to host your code and a live demo.

## Phase 1: Hosting the Code (GitHub)
1.  **Initialize Git**: If you haven't already, open a terminal in the project root:
    ```bash
    git init
    git add .
    git commit -m "Initial commit: Conference Submission v1.0"
    ```
2.  **Create Repository**: Go to [GitHub](https://github.com/new) and create a new repository called `CLINIX.AI`.
3.  **Push Code**:
    ```bash
    git remote add origin https://github.com/[your-username]/CLINIX.AI.git
    git branch -M main
    git push -u origin main
    ```

## Phase 2: Live Prototype (GitHub Pages)
The **Standalone** version is perfect for a quick, zero-server demo.
1.  In your GitHub repository, go to **Settings** > **Pages**.
2.  Under **Build and deployment**, set the source to **Deploy from a branch**.
3.  Select the `main` branch and the `/ (root)` folder (or Move the `standalone` folder to the root if you only want the demo).
4.  **Save**. Your link will be: `https://[your-username].github.io/CLINIX.AI/standalone/`.

## Phase 3: Full Application (Render)
To host the full Spring Boot application (with database and login):
1.  Connect your GitHub repository to **Render.com**.
2.  Create a **New Web Service**.
3.  Render will automatically detect the `Dockerfile`.
4.  Set the **Environment Variable**: `PORT=8080`.
5.  **Deploy**. Your link will be: `https://clinix-ai.onrender.com`.

---
**Recommendation**: For your conference paper, provide both the **GitHub URL** (for the source code) and the **GitHub Pages URL** (for the interactive demo).
