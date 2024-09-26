document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('investment-form');
    const resultSection = document.getElementById('stock-list');

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const amount = document.getElementById('amount').value;
        const days = document.getElementById('day').value;

        if (!amount || days === 0) {
            alert('Please fill in all fields');
            return;
        }

        // Prepare the investment data
        const investmentData = { amount: parseFloat(amount) };

        try {
            // Send a POST request to the backend, with 'daysOfInvestment' as a query parameter
            const response = await fetch(`/investment/calculate?daysOfInvestment=${days}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(investmentData)  // Send investment amount in the body
            });

            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const data = await response.json();
                displayResults(data);
            } else {
                const errorText = await response.text();
                alert("Error: Something went wrong. Please check the backend logs.");
            }
        } catch (error) {
            console.error('Error:', error);
            alert('An error occurred during the calculation.');
        }
    });

    // Function to display results
    function displayResults(data) {
        resultSection.innerHTML = '';

        if (data.stocks && data.stocks.length > 0) {
            data.stocks.forEach(stock => {
                const stockItem = document.createElement('div');
                stockItem.classList.add('stock-item');

                stockItem.innerHTML = `
                    <h4>Stock: ${stock.stockSymbol}</h4>
                    <p>VaR: ${stock.varValue}</p>
                    <p>Confidence Level: ${stock.confidenceLevel}%</p>
                    <p>Initial Price: ${stock.initialPrice}</p>
                `;

                resultSection.appendChild(stockItem);
            });
        } else if (data.error) {
            const errorMsg = document.createElement('p');
            errorMsg.textContent = `Error: ${data.error}`;
            resultSection.appendChild(errorMsg);
        } else {
            resultSection.innerHTML = '<p>No stocks matched your criteria.</p>';
        }
    }
});
