import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface AdCatalogItem {
  id: string;
  name: string;
}

export interface AdCatalogsResponse {
  categories: AdCatalogItem[];
  placements: AdCatalogItem[];
  event_types: AdCatalogItem[];
}

export interface Advertiser {
  id: string;
  name: string;
  category: string;
  contact_name?: string | null;
  email?: string | null;
  phone?: string | null;
  whatsapp?: string | null;
  website?: string | null;
  logo_url?: string | null;
  logo_public_id?: string | null;
  is_government: boolean;
  official_source_url?: string | null;
  veterinary_id?: string | null;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface CreateAdvertiserRequest {
  name: string;
  category: string;
  contact_name?: string | null;
  email?: string | null;
  phone?: string | null;
  whatsapp?: string | null;
  website?: string | null;
  logo_url?: string | null;
  logo_public_id?: string | null;
  is_government: boolean;
  official_source_url?: string | null;
  veterinary_id?: string | null;
}

export interface UpdateAdvertiserRequest extends CreateAdvertiserRequest {
  is_active: boolean;
}

export interface AdCampaignSummary {
  id: string;
  advertiser_id: string;
  advertiser_name: string;
  is_government: boolean;
  title: string;
  description?: string | null;
  cta_label: string;
  target_url?: string | null;
  image_url: string;
  image_public_id: string;
  category: string;
  placement: string;
  badge_label: string;
  latitude?: number | null;
  longitude?: number | null;
  geohash?: string | null;
  radius_km?: number | null;
  priority: number;
  starts_at: string;
  ends_at: string;
  is_active: boolean;
  impressions_count: number;
  clicks_count: number;
  ctr_percentage: number;
  created_at: string;
  updated_at: string;
}

export interface CreateAdCampaignRequest {
  advertiser_id: string;
  title: string;
  description?: string | null;
  cta_label?: string | null;
  target_url?: string | null;
  image_url: string;
  image_public_id: string;
  category: string;
  placement: string;
  badge_label?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  radius_km?: number | null;
  priority: number;
  starts_at: string;
  ends_at: string;
  is_active: boolean;
}

export interface UpdateAdCampaignRequest {
  title: string;
  description?: string | null;
  cta_label?: string | null;
  target_url?: string | null;
  image_url: string;
  image_public_id: string;
  category: string;
  placement: string;
  badge_label?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  radius_km?: number | null;
  priority: number;
  starts_at: string;
  ends_at: string;
  is_active: boolean;
}

export interface UploadAdImageResponse {
  image_url: string;
  image_public_id: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdsService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/ads`;

  getCatalogs(): Observable<AdCatalogsResponse> {
    return this.http.get<AdCatalogsResponse>(`${this.apiUrl}/catalogs`);
  }

  // --- Advertisers ---
  listAdvertisers(): Observable<Advertiser[]> {
    return this.http.get<Advertiser[]>(`${this.apiUrl}/advertisers`);
  }

  createAdvertiser(payload: CreateAdvertiserRequest): Observable<Advertiser> {
    return this.http.post<Advertiser>(`${this.apiUrl}/advertisers`, payload);
  }

  updateAdvertiser(id: string, payload: UpdateAdvertiserRequest): Observable<Advertiser> {
    return this.http.put<Advertiser>(`${this.apiUrl}/advertisers/${id}`, payload);
  }

  deleteAdvertiser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/advertisers/${id}`);
  }

  // --- Campaigns ---
  listCampaigns(): Observable<AdCampaignSummary[]> {
    return this.http.get<AdCampaignSummary[]>(`${this.apiUrl}/campaigns`);
  }

  getCampaignById(id: string): Observable<AdCampaignSummary> {
    return this.http.get<AdCampaignSummary>(`${this.apiUrl}/campaigns/${id}`);
  }

  createCampaign(payload: CreateAdCampaignRequest): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/campaigns`, payload);
  }

  updateCampaign(id: string, payload: UpdateAdCampaignRequest): Observable<unknown> {
    return this.http.put(`${this.apiUrl}/campaigns/${id}`, payload);
  }

  deleteCampaign(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/campaigns/${id}`);
  }

  uploadAdImage(file: File): Observable<UploadAdImageResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<UploadAdImageResponse>(`${this.apiUrl}/campaigns/upload-image`, formData);
  }
}
