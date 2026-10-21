"""
Gatehouse ML Model Generator & ONNX Exporter
Generates calibrated tabular fraud detection weights and exports an ONNX model.
Run:
    pip install numpy scikit-learn skl2onnx onnx
    python tools/build_model.py
"""
import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

FEATURE_NAMES = [
    "log_amount",
    "amount_vs_usual",
    "txns_last_hour",
    "night_time",
    "foreign_country",
    "unseen_device",
    "merchant_risk",
    "log_distance_km"
]

def generate_synthetic_data(n_samples=10000):
    np.random.seed(42)
    # Synthetic normal & fraud distribution
    is_fraud = np.random.binomial(1, 0.05, n_samples)
    
    # 0: log_amount
    log_amt = np.where(is_fraud, np.random.normal(10.0, 1.5, n_samples), np.random.normal(7.5, 1.2, n_samples))
    # 1: amount_vs_usual
    amt_vs = np.where(is_fraud, np.random.exponential(4.0, n_samples) + 2.0, np.random.exponential(0.8, n_samples) + 0.5)
    # 2: txns_last_hour
    tx_hour = np.where(is_fraud, np.random.poisson(4, n_samples), np.random.poisson(0.3, n_samples))
    # 3: night_time
    night = np.where(is_fraud, np.random.binomial(1, 0.45, n_samples), np.random.binomial(1, 0.12, n_samples))
    # 4: foreign_country
    foreign = np.where(is_fraud, np.random.binomial(1, 0.50, n_samples), np.random.binomial(1, 0.08, n_samples))
    # 5: unseen_device
    unseen = np.where(is_fraud, np.random.binomial(1, 0.70, n_samples), np.random.binomial(1, 0.15, n_samples))
    # 6: merchant_risk (0 to 1)
    m_risk = np.where(is_fraud, np.random.beta(5, 2, n_samples), np.random.beta(1, 4, n_samples))
    # 7: log_distance_km
    log_dist = np.where(is_fraud, np.random.normal(7.5, 2.0, n_samples), np.random.normal(2.0, 1.5, n_samples))

    X = np.column_stack([log_amt, amt_vs, tx_hour, night, foreign, unseen, m_risk, log_dist]).astype(np.float32)
    y = is_fraud.astype(np.int64)
    return X, y

def main():
    print("Generating simulated transaction dataset...")
    X, y = generate_synthetic_data(25000)
    print(f"Dataset generated: {X.shape[0]} rows, {np.mean(y)*100:.2f}% fraud incidence.")

    pipeline = Pipeline([
        ('scaler', StandardScaler()),
        ('classifier', LogisticRegression(class_weight='balanced', max_iter=1000, random_state=42))
    ])
    pipeline.fit(X, y)
    print("Model fitted successfully.")

    try:
        from skl2onnx import convert_sklearn
        from skl2onnx.common.data_types import FloatTensorType
        
        initial_type = [('features', FloatTensorType([None, 8]))]
        onnx_model = convert_sklearn(pipeline, initial_types=initial_type, target_opset=14)
        
        out_path = "src/main/resources/fraud_detector.onnx"
        with open(out_path, "wb") as f:
            f.write(onnx_model.SerializeToString())
        print(f"Exported ONNX model successfully to {out_path}!")
    except ImportError:
        print("Note: Install 'skl2onnx' and 'onnx' to export to .onnx format directly.")

if __name__ == "__main__":
    main()
