# HazardHawk Cost Analysis & Unit Economics

## Executive Summary
Based on current AI Vision API pricing and infrastructure costs, HazardHawk's unit economics are favorable with healthy margins at scale. Processing 10-30 photos daily per user costs approximately $3-9/month in AI processing, making a $29-49/month pricing model profitable.

## AI Vision API Costs (2024-2025)

### Option 1: Google Gemini Vision
**Gemini 1.5 Flash (Recommended for cost-efficiency)**
- ~258 tokens per image (1024x1024)
- Input: $0.075 per 1M tokens
- Output: $0.30 per 1M tokens
- **Cost per photo analysis: ~$0.00003 input + ~$0.0003 output = ~$0.00033/photo**
- With batch processing (50% discount): **~$0.00017/photo**

**Gemini 1.5 Pro (Higher accuracy)**
- Input: $1.25 per 1M tokens (under 128K context)
- Output: $5.00 per 1M tokens
- **Cost per photo analysis: ~$0.0003 input + ~$0.005 output = ~$0.0053/photo**
- With batch processing: **~$0.00265/photo**

### Option 2: OpenAI GPT-4 Vision
**GPT-4o-mini (Most cost-effective)**
- Input: $0.15 per 1M tokens
- Output: $0.60 per 1M tokens
- **Cost per photo (576x1024): ~$0.00765**

**GPT-4o (Premium accuracy)**
- Input: $2.50 per 1M tokens
- Output: $10.00 per 1M tokens
- **Cost per photo: ~$0.025-0.03**

### Option 3: Anthropic Claude 3.5 Sonnet
- Input: $3 per 1M tokens
- Output: $15 per 1M tokens
- **Cost per photo: ~$0.015-0.02**

## Storage Costs (AWS S3)

### Photo Storage Requirements
- Average construction photo size: 2-5 MB (compressed JPEG)
- With metadata and thumbnails: ~6 MB total per photo
- Daily photos per user: 10-30
- Monthly storage per user: 1.8-5.4 GB

### S3 Storage Pricing
**S3 Standard (Active photos - last 30 days)**
- $0.023 per GB/month
- Cost per user: $0.04-0.12/month

**S3 Intelligent-Tiering (Automatic optimization)**
- Monitoring: $0.0025 per 1,000 objects
- Storage: $0.023 per GB (frequent) → $0.0125 per GB (infrequent after 30 days)
- Cost per user: $0.03-0.09/month

**S3 Glacier Instant Retrieval (Photos > 90 days)**
- $0.004 per GB/month
- Cost per user for archives: $0.01-0.03/month

### Data Transfer Costs
- Upload (from mobile): Free
- Download to app: $0.09 per GB (first 10TB/month)
- CDN via CloudFront: $0.085 per GB

## Database & Infrastructure Costs

### PostgreSQL Database (AWS RDS)
**Development/Small (< 1,000 users)**
- db.t3.micro: $12.41/month
- Storage (100GB): $11.50/month
- **Total: ~$24/month**

**Growth (1,000-10,000 users)**
- db.t3.large: $59.52/month
- Storage (500GB): $57.50/month
- Read replicas: $59.52/month
- **Total: ~$177/month**

**Scale (10,000-100,000 users)**
- db.r6g.xlarge: $183.60/month
- Storage (2TB): $230/month
- Multi-AZ + Read replicas: $550/month
- **Total: ~$964/month**

### Application Servers (AWS EC2/ECS)
**Small Scale**
- 2x t3.medium: $60/month
- Load balancer: $20/month
- **Total: $80/month**

**Medium Scale**
- 4x t3.large: $240/month
- Load balancer: $20/month
- Auto-scaling: Variable
- **Total: $260-400/month**

**Large Scale**
- ECS Fargate or EKS
- **Estimated: $2,000-5,000/month**

## Total Cost Per User Analysis

### Per User Per Month Costs

#### Light User (10 photos/day)
- AI Processing (Gemini Flash batch): 10 × 30 × $0.00017 = **$0.051**
- Storage (1.8GB): **$0.04**
- Database allocation: **$0.02**
- Infrastructure share: **$0.08**
- **Total: ~$0.19/month**

#### Average User (20 photos/day)
- AI Processing: 20 × 30 × $0.00017 = **$0.102**
- Storage (3.6GB): **$0.08**
- Database allocation: **$0.02**
- Infrastructure share: **$0.08**
- **Total: ~$0.28/month**

#### Heavy User (30 photos/day)
- AI Processing: 30 × 30 × $0.00017 = **$0.153**
- Storage (5.4GB): **$0.12**
- Database allocation: **$0.02**
- Infrastructure share: **$0.08**
- **Total: ~$0.37/month**

### If Using Premium AI (GPT-4o)
#### Average User (20 photos/day)
- AI Processing: 20 × 30 × $0.00765 = **$4.59**
- Storage & Infrastructure: **$0.18**
- **Total: ~$4.77/month**

## Pricing Strategy Recommendations

### Tiered Pricing Model (No Free Tier)

#### Starter - $49/user/month
- 15 photos/day (450/month)
- Basic AI analysis (Gemini Flash)
- 30-day photo storage
- Gross margin: ~93% ($27 profit)

#### Professional - $99/user/month
- 30 photos/day (900/month)
- Advanced AI analysis (Gemini Pro)
- 90-day photo storage
- Priority processing
- Gross margin: ~89% ($43.50 profit)

#### Team - $79/user/month (5+ users)
- Unlimited photos
- Premium AI (GPT-4o for complex cases)
- 1-year storage
- Admin dashboard
- Gross margin at 20 photos/day avg: ~85% ($33 profit)

#### Enterprise - Custom
- Volume pricing
- API access
- Custom AI models
- Unlimited storage
- Target margin: 70-80%

## Scale Economics

### 1,000 Customers
**Monthly Costs:**
- AI Processing: ~$280
- Storage: ~$100
- Database: ~$177
- Infrastructure: ~$400
- **Total: $957**

**Revenue (assuming mix):**
- 400 Starter @ $29: $11,600
- 500 Professional @ $49: $24,500
- 100 Team (avg 10 users) @ $39: $39,000
- **Total Revenue: $75,100**
- **Gross Margin: 98.7%**

### 10,000 Customers
**Monthly Costs:**
- AI Processing: ~$2,800
- Storage: ~$1,000
- Database: ~$964
- Infrastructure: ~$5,000
- Support/Operations: ~$10,000
- **Total: $19,764**

**Revenue:**
- **Total Revenue: ~$751,000**
- **Gross Margin: 97.4%**

### 100,000 Customers
**Monthly Costs:**
- AI Processing: ~$28,000
- Storage: ~$10,000
- Database: ~$5,000
- Infrastructure: ~$50,000
- Operations: ~$100,000
- **Total: $193,000**

**Revenue:**
- **Total Revenue: ~$7,510,000**
- **Gross Margin: 97.4%**

## Cost Optimization Strategies

### 1. AI Processing Optimization
- Use Gemini Flash for initial analysis (cost: $0.00017/photo)
- Upgrade to Pro/GPT-4 only for complex cases
- Implement smart caching for similar photos
- Batch process during off-peak for 50% discount

### 2. Storage Optimization
- Compress photos on-device before upload
- Auto-archive to Glacier after 90 days
- Implement intelligent thumbnail generation
- Use S3 Intelligent-Tiering

### 3. Infrastructure Optimization
- Use AWS Spot instances for batch processing
- Implement aggressive caching with CloudFront
- Use serverless (Lambda) for variable workloads
- Reserved instances for predictable base load

### 4. Smart AI Routing
```python
def select_ai_model(photo_context):
    if photo_context.is_simple_ppe_check:
        return "gemini_flash_batch"  # $0.00017
    elif photo_context.needs_osha_codes:
        return "gemini_pro_batch"    # $0.00265
    elif photo_context.is_incident:
        return "gpt4o"              # $0.00765
    elif photo_context.is_complex:
        return "claude_sonnet"       # $0.02
```

## Break-Even Analysis

### Customer Acquisition Cost (CAC) Recovery
Assuming $200 CAC:
- Starter ($29): 7 months
- Professional ($49): 4.1 months
- Team ($39 × 10): 0.5 months

### Path to Profitability
With 40% operating expenses:
- Break-even: ~250 customers
- $1M ARR: ~1,700 customers
- $10M ARR: ~17,000 customers

## Conclusion

The unit economics are extremely favorable:
- **Cost per user: $0.20-5.00/month** (depending on usage and AI model)
- **Price per user: $29-49/month**
- **Gross margins: 85-95%**
- **Operating margins at scale: 40-60%**

The key is starting with efficient AI models (Gemini Flash) and scaling to premium models only when necessary. With no free tier, every user contributes to positive unit economics from day one.