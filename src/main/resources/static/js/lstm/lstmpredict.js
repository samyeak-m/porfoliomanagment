document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("predict-form");
    const resultDiv = document.getElementById("predictionResults");

    form.addEventListener("submit", async function (e) {
        e.preventDefault();
        resultDiv.innerHTML = "Predicting...";

        const stockSymbol = document.getElementById("stockSymbol").value.trim();
        if (!stockSymbol) {
            resultDiv.innerHTML = "<span style='color:red'>Please enter a stock symbol.</span>";
            return;
        }

        try {
            const response = await fetch("/api/lstm/predict", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ stockSymbol })
            });
            if (response.ok) {
                const data = await response.json();
                resultDiv.innerHTML = `
                    <h3>Prediction Result</h3>
                    <p><strong>Stock Symbol:</strong> ${data.stockSymbol}</p>
                    <p><strong>Predicted Price:</strong> ${data.prediction.toFixed(2)}</p>
                    <p><strong>Last Close:</strong> ${data.lastClose.toFixed(2)}</p>
                    <p><strong>Point Change:</strong> ${data.pointChange.toFixed(2)}</p>
                    <p><strong>Price Change (%):</strong> ${data.priceChange.toFixed(2)}</p>
                `;
            } else {
                resultDiv.innerHTML = "<span style='color:red'>Prediction failed. Please try again.</span>";
            }
        } catch (err) {
            resultDiv.innerHTML = "<span style='color:red'>Error connecting to server.</span>";
        }
    });
});