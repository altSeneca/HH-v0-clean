#!/usr/bin/env python3
"""
Simple script to authenticate with Hugging Face
Usage: python hf_login.py YOUR_TOKEN_HERE
"""
import sys
from huggingface_hub import login

if len(sys.argv) != 2:
    print("Usage: python hf_login.py YOUR_HF_TOKEN")
    print("Get your token from: https://huggingface.co/settings/tokens")
    sys.exit(1)

token = sys.argv[1]

try:
    login(token=token)
    print("✅ Successfully logged in to Hugging Face!")
    print("🎉 You can now access gated models like Gemma")
except Exception as e:
    print(f"❌ Login failed: {e}")
    sys.exit(1)