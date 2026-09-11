from flask import Flask, request, jsonify
import requests

app = Flask(__name__)

@app.route("/ai", methods=["POST"])
def ai():
    data = request.json
    prompt = data["prompt"]

    response = requests.post("http://localhost:11434/api/generate", json={
        "model": "llama3",
        "prompt": prompt,
        "stream": False
    })

    return jsonify(response.json())

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
