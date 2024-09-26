document.getElementById('predictionForm').addEventListener('submit', function(e) {
    e.preventDefault();

    const stockSymbols = document.getElementById('stockSymbols').value;

    if (stockSymbols.trim() === "") {
        alert("Please enter at least one stock symbol.");
        return;
    }

    fetch(`/lstm?stockSymbols=${encodeURIComponent(stockSymbols)}`)
        .then(response => response.text())
        .then(data => {
            document.getElementById('predictionResults').innerHTML = data;
        })
        .catch(error => {
            console.error('Error fetching predictions:', error);
            document.getElementById('predictionResults').innerHTML = "An error occurred while predicting stocks.";
        });
});
